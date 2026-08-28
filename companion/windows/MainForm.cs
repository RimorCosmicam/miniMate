using NAudio.CoreAudioApi;
using Windows.Devices.Enumeration;
using Zeroconf;

namespace MiniMateAudio;

internal sealed class MainForm : Form
{
    private readonly BridgeClient bridge = new();
    private readonly DesktopAudioCapture capture = new();
    private readonly PhoneMicrophoneOutput micOutput = new();
    private readonly Label status = new() { AutoSize = true, Text = "Looking for MiniMate…", ForeColor = Color.DimGray };
    private readonly ComboBox wifi = new() { DropDownStyle = ComboBoxStyle.DropDownList, Width = 270 };
    private readonly ComboBox bluetooth = new() { DropDownStyle = ComboBoxStyle.DropDownList, Width = 270 };
    private readonly ComboBox micDevice = new() { DropDownStyle = ComboBoxStyle.DropDownList, Width = 270 };
    private readonly Button stream = new() { Text = "Start audio", AutoSize = true, Enabled = false };
    private readonly NotifyIcon tray;
    private IReadOnlyList<IZeroconfHost> wifiHosts = [];
    private IReadOnlyList<DeviceInformation> bluetoothDevices = [];
    private int sequence;
    private bool isStreaming;
    private bool wifiMode = true;

    public MainForm()
    {
        Text = "MiniMate Audio"; Width = 440; Height = 430; MinimumSize = new Size(440, 430); StartPosition = FormStartPosition.CenterScreen;
        var iconPath = Path.Combine(AppContext.BaseDirectory, "Assets", "Mini.png");
        if (File.Exists(iconPath)) { using var bitmap = new Bitmap(iconPath); Icon = Icon.FromHandle(bitmap.GetHicon()); }
        tray = new NotifyIcon { Icon = Icon, Text = "MiniMate Audio", Visible = true, ContextMenuStrip = new ContextMenuStrip() };
        tray.ContextMenuStrip.Items.Add("Open", null, (_, _) => ShowFromTray());
        tray.ContextMenuStrip.Items.Add("Quit", null, (_, _) => { tray.Visible = false; Application.Exit(); });
        tray.DoubleClick += (_, _) => ShowFromTray();

        var root = new FlowLayoutPanel { Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false, Padding = new Padding(24), AutoScroll = true };
        root.Controls.Add(new Label { Text = "MiniMate Audio", Font = new Font((SystemFonts.MessageBoxFont ?? SystemFonts.DefaultFont).FontFamily, 20, FontStyle.Bold), AutoSize = true });
        root.Controls.Add(status);
        root.Controls.Add(Section("Wi-Fi · lossless 24-bit / 48 kHz", wifi, "Connect", async () => await ConnectWifi()));
        root.Controls.Add(Section("Bluetooth · fallback", bluetooth, "Connect", async () => await ConnectBluetooth()));
        root.Controls.Add(Section("Phone microphone destination", micDevice, "Use device", () => { if (micDevice.SelectedItem is MMDevice d) micOutput.Select(d); return Task.CompletedTask; }));
        root.Controls.Add(new Label { Text = "Choose VB-CABLE Input above; apps can then select VB-CABLE Output as their microphone.", AutoSize = true, MaximumSize = new Size(360, 0), ForeColor = Color.DimGray });
        var buttons = new FlowLayoutPanel { AutoSize = true };
        stream.Click += (_, _) => ToggleStreaming(); buttons.Controls.Add(stream);
        var disconnect = new Button { Text = "Disconnect", AutoSize = true }; disconnect.Click += async (_, _) => { StopStreaming(); await bridge.DisconnectAsync(); status.Text = "Disconnected"; }; buttons.Controls.Add(disconnect);
        root.Controls.Add(buttons); Controls.Add(root);

        bridge.StatusChanged += value => BeginInvoke(() => { status.Text = value; stream.Enabled = value.Contains("connected", StringComparison.OrdinalIgnoreCase); });
        bridge.FrameReceived += frame => { if (frame.Type == Protocol.Microphone) micOutput.Push(frame); };
        capture.PacketReady += packet => _ = SendPacket(packet);
        FormClosing += (_, e) => { if (e.CloseReason == CloseReason.UserClosing) { e.Cancel = true; Hide(); } };
        Load += async (_, _) => await Discover();
    }

    private Control Section(string title, ComboBox combo, string action, Func<Task> click)
    {
        var group = new GroupBox { Text = title, Width = 370, Height = 82, Padding = new Padding(10) };
        combo.Location = new Point(12, 30); group.Controls.Add(combo);
        var button = new Button { Text = action, AutoSize = true, Location = new Point(290, 28) }; button.Click += async (_, _) => await click(); group.Controls.Add(button);
        return group;
    }

    private async Task Discover()
    {
        try {
            wifiHosts = await ZeroconfResolver.ResolveAsync("_minimate-audio._tcp.local.", TimeSpan.FromSeconds(4));
            wifi.DataSource = wifiHosts.ToList(); wifi.DisplayMember = nameof(IZeroconfHost.DisplayName);
        } catch (Exception e) { status.Text = $"Wi-Fi discovery: {e.Message}"; }
        try {
            bluetoothDevices = await BridgeClient.FindBluetoothAsync();
            bluetooth.DataSource = bluetoothDevices.ToList(); bluetooth.DisplayMember = nameof(DeviceInformation.Name);
        } catch { }
        micDevice.DataSource = PhoneMicrophoneOutput.Devices.ToList(); micDevice.DisplayMember = nameof(MMDevice.FriendlyName);
    }

    private async Task ConnectWifi()
    {
        if (wifi.SelectedItem is not IZeroconfHost host) return;
        var service = host.Services.Values.First(); wifiMode = true;
        await bridge.ConnectWifiAsync(host.IPAddress, service.Port); stream.Enabled = true; ToggleStreaming();
    }
    private async Task ConnectBluetooth()
    {
        if (bluetooth.SelectedItem is not DeviceInformation device) return;
        wifiMode = false; await bridge.ConnectBluetoothAsync(device); stream.Enabled = true; ToggleStreaming();
    }
    private void ToggleStreaming() { if (isStreaming) StopStreaming(); else { capture.Start(); isStreaming = true; stream.Text = "Stop audio"; } }
    private void StopStreaming() { capture.Stop(); isStreaming = false; stream.Text = "Start audio"; }
    private async Task SendPacket(byte[] pcm24)
    {
        Protocol.Frame frame;
        if (wifiMode) frame = new(Protocol.Playback, Protocol.Pcm24, 2, 48_000, sequence++, pcm24);
        else {
            var frames48 = pcm24.Length / 6; var samples = new short[frames48 * 2 / 3 * 2];
            for (var f = 0; f < frames48 * 2 / 3; f++) for (var c = 0; c < 2; c++) { var o = (f * 3 / 2) * 6 + c * 3; var v = pcm24[o] | pcm24[o + 1] << 8 | pcm24[o + 2] << 16; if ((v & 0x800000) != 0) v |= unchecked((int)0xFF000000); samples[f * 2 + c] = (short)(v >> 8); }
            frame = new(Protocol.Playback, Protocol.Ima, 2, 32_000, sequence++, ImaAdpcm.Encode(samples, 2));
        }
        try { await bridge.SendAsync(frame); } catch { }
    }
    private void ShowFromTray() { Show(); WindowState = FormWindowState.Normal; Activate(); }

    protected override void Dispose(bool disposing)
    {
        if (disposing) { tray.Dispose(); capture.Dispose(); micOutput.Dispose(); bridge.DisposeAsync().AsTask().GetAwaiter().GetResult(); }
        base.Dispose(disposing);
    }
}
