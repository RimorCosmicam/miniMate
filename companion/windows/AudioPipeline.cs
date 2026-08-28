using NAudio.CoreAudioApi;
using NAudio.Wave;
using NAudio.Wave.SampleProviders;

namespace MiniMateAudio;

internal sealed class DesktopAudioCapture : IDisposable
{
    public event Action<byte[]>? PacketReady;
    private WasapiLoopbackCapture? capture;
    private BufferedWaveProvider? buffered;
    private CancellationTokenSource? cancellation;

    public void Start()
    {
        if (capture != null) return;
        capture = new WasapiLoopbackCapture();
        buffered = new BufferedWaveProvider(capture.WaveFormat) { DiscardOnBufferOverflow = true, BufferDuration = TimeSpan.FromMilliseconds(250) };
        capture.DataAvailable += (_, e) => buffered.AddSamples(e.Buffer, 0, e.BytesRecorded);
        capture.StartRecording();
        cancellation = new CancellationTokenSource();
        _ = Task.Run(() => Pump(cancellation.Token));
    }

    private async Task Pump(CancellationToken token)
    {
        var source = buffered!.ToSampleProvider();
        if (source.WaveFormat.Channels == 1) source = new MonoToStereoSampleProvider(source);
        else if (source.WaveFormat.Channels > 2) source = new WdlResamplingSampleProvider(new MultiplexingSampleProvider([source], 2), 48_000);
        if (source.WaveFormat.SampleRate != 48_000) source = new WdlResamplingSampleProvider(source, 48_000);
        var samples = new float[960 * 2];
        while (!token.IsCancellationRequested) {
            var read = source.Read(samples, 0, samples.Length);
            if (read < samples.Length) { await Task.Delay(3, token); continue; }
            var bytes = new byte[samples.Length * 3];
            for (var i = 0; i < samples.Length; i++) {
                var value = (int)Math.Round(Math.Clamp(samples[i], -1f, 0.9999999f) * 8_388_608);
                bytes[i * 3] = (byte)value; bytes[i * 3 + 1] = (byte)(value >> 8); bytes[i * 3 + 2] = (byte)(value >> 16);
            }
            PacketReady?.Invoke(bytes);
        }
    }

    public void Stop()
    {
        cancellation?.Cancel(); cancellation?.Dispose(); cancellation = null;
        capture?.StopRecording(); capture?.Dispose(); capture = null; buffered = null;
    }
    public void Dispose() => Stop();
}

internal sealed class PhoneMicrophoneOutput : IDisposable
{
    private WasapiOut? output;
    private BufferedWaveProvider? provider;
    public static IReadOnlyList<MMDevice> Devices => new MMDeviceEnumerator().EnumerateAudioEndPoints(DataFlow.Render, DeviceState.Active).ToList();

    public void Select(MMDevice device)
    {
        Dispose();
        provider = new BufferedWaveProvider(new WaveFormat(48_000, 16, 1)) { DiscardOnBufferOverflow = true, BufferDuration = TimeSpan.FromMilliseconds(200) };
        output = new WasapiOut(device, NAudio.CoreAudioApi.AudioClientShareMode.Shared, true, 35);
        output.Init(provider); output.Play();
    }

    public void Push(Protocol.Frame frame)
    {
        if (provider == null) return;
        byte[] bytes;
        if (frame.Codec == Protocol.Pcm16) bytes = frame.Payload;
        else if (frame.Codec == Protocol.Ima) {
            var samples = ImaAdpcm.Decode(frame.Payload, 1); bytes = new byte[samples.Length * 2]; Buffer.BlockCopy(samples, 0, bytes, 0, bytes.Length);
        } else return;
        provider.AddSamples(bytes, 0, bytes.Length);
    }

    public void Dispose() { output?.Stop(); output?.Dispose(); output = null; provider = null; }
}
