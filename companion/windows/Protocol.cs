using System.Buffers.Binary;

namespace MiniMateAudio;

internal static class Protocol
{
    public const uint Magic = 0x4D4D4155;
    public const byte Version = 1;
    public static readonly Guid ServiceUuid = Guid.Parse("f7f8c3a4-6bc7-4b7a-9c35-3b41af96e9d2");
    public const int WifiPort = 42308;
    public const byte Playback = 1, Microphone = 2, Hello = 3, State = 4, Ping = 5;
    public const byte Pcm16 = 0, Ima = 1, Pcm24 = 2;

    public sealed record Frame(byte Type, byte Codec, byte Channels, int SampleRate, int Sequence, byte[] Payload)
    {
        public byte[] Encode()
        {
            var result = new byte[20 + Payload.Length];
            BinaryPrimitives.WriteUInt32BigEndian(result, Magic);
            result[4] = Version; result[5] = Type; result[6] = Codec; result[7] = Channels;
            BinaryPrimitives.WriteInt32BigEndian(result.AsSpan(8), SampleRate);
            BinaryPrimitives.WriteInt32BigEndian(result.AsSpan(12), Sequence);
            BinaryPrimitives.WriteInt32BigEndian(result.AsSpan(16), Payload.Length);
            Payload.CopyTo(result, 20);
            return result;
        }
    }

    public static async Task<Frame> ReadAsync(Stream stream, CancellationToken token)
    {
        var header = new byte[20];
        await stream.ReadExactlyAsync(header, token);
        if (BinaryPrimitives.ReadUInt32BigEndian(header) != Magic || header[4] != Version) throw new InvalidDataException("Invalid MiniMate frame");
        var length = BinaryPrimitives.ReadInt32BigEndian(header.AsSpan(16));
        if (length is < 0 or > 65536) throw new InvalidDataException("Invalid MiniMate payload");
        var payload = new byte[length];
        await stream.ReadExactlyAsync(payload, token);
        return new Frame(header[5], header[6], header[7], BinaryPrimitives.ReadInt32BigEndian(header.AsSpan(8)),
            BinaryPrimitives.ReadInt32BigEndian(header.AsSpan(12)), payload);
    }
}

internal static class ImaAdpcm
{
    private static readonly int[] Steps = [7,8,9,10,11,12,13,14,16,17,19,21,23,25,28,31,34,37,41,45,50,55,60,66,73,80,88,97,107,118,130,143,157,173,190,209,230,253,279,307,337,371,408,449,494,544,598,658,724,796,876,963,1060,1166,1282,1411,1552,1707,1878,2066,2272,2499,2749,3024,3327,3660,4026,4428,4871,5358,5894,6484,7132,7845,8630,9493,10442,11487,12635,13899,15289,16818,18500,20350,22385,24623,27086,29794,32767];
    private static readonly int[] Indexes = [-1,-1,-1,-1,2,4,6,8];

    public static byte[] Encode(short[] samples, int channels)
    {
        var frames = samples.Length / channels;
        var predictor = Enumerable.Range(0, channels).Select(i => (int)samples[i]).ToArray();
        var index = new int[channels];
        if (frames > 1) for (var c = 0; c < channels; c++) {
            var delta = Math.Abs(samples[channels + c] - predictor[c]);
            while (index[c] < 88 && Steps[index[c]] < delta) index[c]++;
            index[c] = Math.Max(0, index[c] - 3);
        }
        var output = new List<byte>(channels * 4 + samples.Length / 2);
        for (var c = 0; c < channels; c++) { output.Add((byte)predictor[c]); output.Add((byte)(predictor[c] >> 8)); output.Add((byte)index[c]); output.Add(0); }
        int? pending = null;
        for (var frame = 1; frame < frames; frame++) for (var c = 0; c < channels; c++) {
            var nibble = EncodeOne(samples[frame * channels + c], c, predictor, index);
            if (pending.HasValue) { output.Add((byte)(pending.Value | nibble << 4)); pending = null; } else pending = nibble;
        }
        if (pending.HasValue) output.Add((byte)pending.Value);
        return output.ToArray();
    }

    public static short[] Decode(byte[] data, int channels)
    {
        var predictor = new int[channels]; var index = new int[channels];
        for (var c = 0; c < channels; c++) { predictor[c] = (short)(data[c * 4] | data[c * 4 + 1] << 8); index[c] = Math.Min(88, (int)data[c * 4 + 2]); }
        var count = (data.Length - channels * 4) * 2 / channels * channels;
        var result = new short[count + channels];
        for (var c = 0; c < channels; c++) result[c] = (short)predictor[c];
        for (var n = 0; n < count; n++) {
            var packed = data[channels * 4 + n / 2]; var nibble = n % 2 == 0 ? packed & 15 : packed >> 4; var c = n % channels;
            result[channels + n] = (short)DecodeOne(nibble, c, predictor, index);
        }
        return result;
    }

    private static int EncodeOne(int sample, int c, int[] predictor, int[] index) {
        var delta = sample - predictor[c]; var nibble = 0; if (delta < 0) { nibble = 8; delta = -delta; }
        var step = Steps[index[c]]; var diff = step >> 3;
        if (delta >= step) { nibble |= 4; delta -= step; diff += step; }
        if (delta >= step >> 1) { nibble |= 2; delta -= step >> 1; diff += step >> 1; }
        if (delta >= step >> 2) { nibble |= 1; diff += step >> 2; }
        predictor[c] = Math.Clamp(predictor[c] + ((nibble & 8) == 0 ? diff : -diff), short.MinValue, short.MaxValue);
        index[c] = Math.Clamp(index[c] + Indexes[nibble & 7], 0, 88); return nibble;
    }
    private static int DecodeOne(int nibble, int c, int[] predictor, int[] index) {
        var step = Steps[index[c]]; var diff = step >> 3;
        if ((nibble & 4) != 0) diff += step; if ((nibble & 2) != 0) diff += step >> 1; if ((nibble & 1) != 0) diff += step >> 2;
        predictor[c] = Math.Clamp(predictor[c] + ((nibble & 8) == 0 ? diff : -diff), short.MinValue, short.MaxValue);
        index[c] = Math.Clamp(index[c] + Indexes[nibble & 7], 0, 88); return predictor[c];
    }
}
