using System.Net.Sockets;
using Windows.Devices.Bluetooth.Rfcomm;
using Windows.Devices.Enumeration;
using Windows.Networking.Sockets;

namespace MiniMateAudio;

internal sealed class BridgeClient : IAsyncDisposable
{
    public event Action<Protocol.Frame>? FrameReceived;
    public event Action<string>? StatusChanged;
    private Stream? stream;
    private IDisposable? owner;
    private CancellationTokenSource? cancellation;
    private readonly SemaphoreSlim writeLock = new(1, 1);

    public async Task ConnectWifiAsync(string host, int port = Protocol.WifiPort)
    {
        await DisconnectAsync();
        var tcp = new TcpClient { NoDelay = true };
        await tcp.ConnectAsync(host, port);
        owner = tcp; stream = tcp.GetStream(); StartReader(); StatusChanged?.Invoke("Lossless Wi-Fi connected");
    }

    public static async Task<IReadOnlyList<DeviceInformation>> FindBluetoothAsync()
    {
        var selector = RfcommDeviceService.GetDeviceSelector(RfcommServiceId.FromUuid(Protocol.ServiceUuid));
        return (await DeviceInformation.FindAllAsync(selector)).ToList();
    }

    public async Task ConnectBluetoothAsync(DeviceInformation device)
    {
        await DisconnectAsync();
        var service = await RfcommDeviceService.FromIdAsync(device.Id) ?? throw new IOException("MiniMate Bluetooth service unavailable");
        var socket = new StreamSocket();
        await socket.ConnectAsync(service.ConnectionHostName, service.ConnectionServiceName, SocketProtectionLevel.BluetoothEncryptionAllowNullAuthentication);
        owner = new CompositeDisposable(socket, service);
        stream = socket.InputStream.AsStreamForRead();
        output = socket.OutputStream.AsStreamForWrite();
        StartReader(); StatusChanged?.Invoke("Bluetooth connected");
    }

    private Stream? output;
    private Stream Output => output ?? stream ?? throw new InvalidOperationException("Not connected");

    public async Task SendAsync(Protocol.Frame frame)
    {
        var bytes = frame.Encode(); await writeLock.WaitAsync();
        try { await Output.WriteAsync(bytes); await Output.FlushAsync(); } finally { writeLock.Release(); }
    }

    private void StartReader()
    {
        cancellation = new CancellationTokenSource();
        _ = Task.Run(async () => {
            try { while (!cancellation.IsCancellationRequested) FrameReceived?.Invoke(await Protocol.ReadAsync(stream!, cancellation.Token)); }
            catch (Exception e) when (e is not OperationCanceledException) { StatusChanged?.Invoke($"Disconnected: {e.Message}"); }
        });
    }

    public async Task DisconnectAsync()
    {
        cancellation?.Cancel(); cancellation?.Dispose(); cancellation = null;
        if (stream != null) await stream.DisposeAsync();
        if (output != null && output != stream) await output.DisposeAsync();
        stream = null; output = null; owner?.Dispose(); owner = null;
    }

    public async ValueTask DisposeAsync() { await DisconnectAsync(); writeLock.Dispose(); }

    private sealed class CompositeDisposable(params IDisposable[] values) : IDisposable { public void Dispose() { foreach (var value in values) value.Dispose(); } }
}
