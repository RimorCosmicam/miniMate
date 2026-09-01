using System.Drawing.Text;
using System.Runtime.InteropServices;
using NAudio.CoreAudioApi;
using Windows.Devices.Enumeration;
using Zeroconf;

namespace MiniMateAudio;

/// <summary>
/// Mont, carried in the application's own folder.
/// </summary>
/// <remarks>
/// Installed fonts cannot be relied on and a private collection costs nothing, so both desktops
/// ship the same two files and neither is typeset by whatever happens to be on the machine.
/// </remarks>
internal static class MontFont
{
    private static readonly PrivateFontCollection Collection = new();
    // The pinned bytes have to outlive the collection that was handed their address, so they are
    // held here for the life of the process rather than left to be collected.
    private static readonly List<GCHandle> Pinned = [];

    static MontFont()
    {
        foreach (var name in new[] { "mont_thin.ttf", "mont_black.ttf" })
        {
            try
            {
                using var stream = typeof(MontFont).Assembly.GetManifestResourceStream(name);
                if (stream is null) continue;
                var bytes = new byte[stream.Length];
                stream.ReadExactly(bytes);
                var handle = GCHandle.Alloc(bytes, GCHandleType.Pinned);
                Pinned.Add(handle);
                Collection.AddMemoryFont(handle.AddrOfPinnedObject(), bytes.Length);
            }
            catch { }
        }
    }

    private static FontFamily? Family(string name) =>
        Collection.Families.FirstOrDefault(f => f.Name == name);

    public static Font Thin(float size) => Make("Mont Thin", size);
    public static Font Black(float size) => Make("Mont Black", size);

    private static Font Make(string name, float size)
    {
        var family = Family(name);
        if (family is not null)
        {
            try { return new Font(family, size, FontStyle.Regular, GraphicsUnit.Point); } catch { }
        }
        var fallback = (SystemFonts.MessageBoxFont ?? SystemFonts.DefaultFont).FontFamily;
        return new Font(fallback, size, name.EndsWith("Black") ? FontStyle.Bold : FontStyle.Regular);
    }
}

/// <summary>
/// The companion, set the way the phone is.
/// </summary>
/// <remarks>
/// The mark, the name split across the two ends of the Mont range, and under it the one thing
/// anyone opened this to do — named after what it will do it to. Everything is a line of text on
/// black, bright when it can be used and dim when it cannot, which is the rule every control on
/// the phone follows and the reason nothing here is drawn as a button. There is no title bar: the
/// window says Close in words, and Ctrl+W closes it because that is what people press.
/// </remarks>
internal sealed class MainForm : Form
{
    private static readonly Color Ink = Color.White;
    private static readonly Color Dim = Color.FromArgb(118, 118, 118);
    private static readonly Color Ground = Color.Black;

    private readonly BridgeClient bridge = new();
    private readonly DesktopAudioCapture capture = new();
    private readonly PhoneMicrophoneOutput micOutput = new();
    private readonly NotifyIcon tray;

    private readonly Label connectRow = Row(13f);
    private readonly Label streamRow = Row(13f);
    private readonly Label micRow = Row(13f);
    private readonly Label micHint = new()
    {
        AutoSize = false, Width = 268, Height = 30, ForeColor = Dim,
        Text = "Pick VB-CABLE Input here, then choose VB-CABLE Output as the microphone in your apps."
    };
    private readonly Label statusRow = new() { AutoSize = true, ForeColor = Dim, Margin = new Padding(0, 10, 0, 2) };
    private readonly Label closeRow = Row(14f);

    private IReadOnlyList<IZeroconfHost> wifiHosts = [];
    private IReadOnlyList<DeviceInformation> bluetoothDevices = [];
    private IReadOnlyList<MMDevice> micDevices = [];
    private int micIndex;
    private int sequence;
    private bool isStreaming;
    private bool connected;
    private bool wifiMode = true;

    public MainForm()
    {
        Text = "MiniMate";
        FormBorderStyle = FormBorderStyle.None;
        BackColor = Ground;
        ClientSize = new Size(340, 330);
        StartPosition = FormStartPosition.CenterScreen;
        KeyPreview = true;

        var iconPath = Path.Combine(AppContext.BaseDirectory, "Assets", "Mini.png");
        Bitmap? mark = File.Exists(iconPath) ? new Bitmap(iconPath) : null;
        if (mark is not null) Icon = Icon.FromHandle(mark.GetHicon());

        tray = new NotifyIcon { Icon = Icon, Text = "MiniMate", Visible = true, ContextMenuStrip = new ContextMenuStrip() };
        tray.ContextMenuStrip.Items.Add("Open", null, (_, _) => ShowFromTray());
        tray.ContextMenuStrip.Items.Add("Quit", null, (_, _) => { tray.Visible = false; Application.Exit(); });
        tray.DoubleClick += (_, _) => ShowFromTray();

        var root = new FlowLayoutPanel
        {
            Dock = DockStyle.Fill, FlowDirection = FlowDirection.TopDown, WrapContents = false,
            BackColor = Ground, Padding = new Padding(22, 26, 18, 16)
        };

        // The mark beside the name, the lightest weight over the heaviest.
        var head = new FlowLayoutPanel { AutoSize = true, FlowDirection = FlowDirection.LeftToRight, WrapContents = false, BackColor = Ground, Margin = new Padding(0, 0, 0, 14) };
        if (mark is not null)
        {
            head.Controls.Add(new PictureBox { Image = mark, SizeMode = PictureBoxSizeMode.Zoom, Size = new Size(44, 44), BackColor = Ground, Margin = new Padding(0, 0, 12, 0) });
        }
        var name = new FlowLayoutPanel { AutoSize = true, FlowDirection = FlowDirection.TopDown, WrapContents = false, BackColor = Ground };
        name.Controls.Add(new Label { Text = "mini", AutoSize = true, ForeColor = Ink, Font = MontFont.Thin(19f), Margin = new Padding(0, 0, 0, -6) });
        name.Controls.Add(new Label { Text = "Mate", AutoSize = true, ForeColor = Ink, Font = MontFont.Black(19f), Margin = new Padding(0) });
        head.Controls.Add(name);
        root.Controls.Add(head);

        connectRow.Click += async (_, _) => await ToggleConnection();
        streamRow.Click += (_, _) => ToggleStreaming();
        micRow.Click += (_, _) => CycleMicrophoneDestination();
        closeRow.Click += (_, _) => Hide();

        micHint.Font = MontFont.Black(7.5f);
        statusRow.Font = MontFont.Black(8f);

        root.Controls.Add(connectRow);
        root.Controls.Add(streamRow);
        root.Controls.Add(micRow);
        root.Controls.Add(micHint);
        root.Controls.Add(statusRow);
        root.Controls.Add(closeRow);
        Controls.Add(root);

        // No title bar to take hold of, so the window is dragged by anything that is not a row.
        EnableDragging(this);
        EnableDragging(root);
        EnableDragging(head);
        EnableDragging(name);

        bridge.StatusChanged += value => BeginInvoke(() =>
        {
            connected = value.Contains("connected", StringComparison.OrdinalIgnoreCase);
            statusRow.Text = value.ToUpperInvariant();
            RefreshRows();
        });
        bridge.FrameReceived += frame => { if (frame.Type == Protocol.Microphone) micOutput.Push(frame); };
        capture.PacketReady += packet => _ = SendPacket(packet);

        // Closing hides to the tray, the way the Mac companion returns to its menu bar.
        FormClosing += (_, e) => { if (e.CloseReason == CloseReason.UserClosing) { e.Cancel = true; Hide(); } };
        Load += async (_, _) => await Discover();
        RefreshRows();
    }

    private static Label Row(float size) => new()
    {
        AutoSize = true, ForeColor = Ink, Font = MontFont.Black(size),
        Margin = new Padding(0, 7, 0, 7), Cursor = Cursors.Hand
    };

    /// <summary>What the connection row says, which is the name of the thing it will act on.</summary>
    private string ConnectionLabel()
    {
        if (connected) return "DISCONNECT";
        if (wifiHosts.Count > 0) return $"CONNECT TO {wifiHosts[0].DisplayName}".ToUpperInvariant();
        if (bluetoothDevices.Count > 0) return $"CONNECT TO {bluetoothDevices[0].Name}".ToUpperInvariant();
        return "LOOKING FOR YOUR PHONE";
    }

    private void RefreshRows()
    {
        var findable = connected || wifiHosts.Count > 0 || bluetoothDevices.Count > 0;
        connectRow.Text = ConnectionLabel();
        connectRow.ForeColor = findable ? Ink : Dim;
        connectRow.Enabled = findable;

        streamRow.Text = isStreaming ? "STOP AUDIO" : "START AUDIO";
        streamRow.ForeColor = connected ? Ink : Dim;
        streamRow.Enabled = connected;

        var destination = micIndex >= 0 && micIndex < micDevices.Count ? micDevices[micIndex].FriendlyName : "no device";
        micRow.Text = $"PHONE MIC TO {destination}".ToUpperInvariant();
        micRow.ForeColor = micDevices.Count > 0 ? Ink : Dim;
        micRow.Enabled = micDevices.Count > 0;

        closeRow.Text = "CLOSE";
        if (string.IsNullOrEmpty(statusRow.Text)) statusRow.Text = "NOT CONNECTED";
    }

    private async Task ToggleConnection()
    {
        if (connected)
        {
            StopStreaming();
            await bridge.DisconnectAsync();
            connected = false;
            statusRow.Text = "DISCONNECTED";
            RefreshRows();
            return;
        }
        // Wi-Fi is the better link, so it is the one tried first; Bluetooth is what is left.
        if (wifiHosts.Count > 0) await ConnectWifi(wifiHosts[0]);
        else if (bluetoothDevices.Count > 0) await ConnectBluetooth(bluetoothDevices[0]);
        else await Discover();
    }

    private void CycleMicrophoneDestination()
    {
        if (micDevices.Count == 0) return;
        micIndex = (micIndex + 1) % micDevices.Count;
        micOutput.Select(micDevices[micIndex]);
        RefreshRows();
    }

    private async Task Discover()
    {
        try
        {
            wifiHosts = await ZeroconfResolver.ResolveAsync("_minimate-audio._tcp.local.", TimeSpan.FromSeconds(4));
        }
        catch (Exception e) { statusRow.Text = $"WI-FI DISCOVERY: {e.Message}".ToUpperInvariant(); }
        try { bluetoothDevices = await BridgeClient.FindBluetoothAsync(); } catch { }
        micDevices = PhoneMicrophoneOutput.Devices.ToList();
        if (micDevices.Count > 0) micOutput.Select(micDevices[micIndex]);
        RefreshRows();
    }

    private async Task ConnectWifi(IZeroconfHost host)
    {
        var service = host.Services.Values.First();
        wifiMode = true;
        await bridge.ConnectWifiAsync(host.IPAddress, service.Port);
        connected = true;
        StartStreaming();
        RefreshRows();
    }

    private async Task ConnectBluetooth(DeviceInformation device)
    {
        wifiMode = false;
        await bridge.ConnectBluetoothAsync(device);
        connected = true;
        StartStreaming();
        RefreshRows();
    }

    private void ToggleStreaming()
    {
        if (isStreaming) StopStreaming(); else StartStreaming();
        RefreshRows();
    }

    private void StartStreaming() { capture.Start(); isStreaming = true; RefreshRows(); }
    private void StopStreaming() { capture.Stop(); isStreaming = false; RefreshRows(); }

    private async Task SendPacket(byte[] pcm24)
    {
        Protocol.Frame frame;
        if (wifiMode) frame = new(Protocol.Playback, Protocol.Pcm24, 2, 48_000, sequence++, pcm24);
        else
        {
            var frames48 = pcm24.Length / 6; var samples = new short[frames48 * 2 / 3 * 2];
            for (var f = 0; f < frames48 * 2 / 3; f++)
                for (var c = 0; c < 2; c++)
                {
                    var o = (f * 3 / 2) * 6 + c * 3;
                    var v = pcm24[o] | pcm24[o + 1] << 8 | pcm24[o + 2] << 16;
                    if ((v & 0x800000) != 0) v |= unchecked((int)0xFF000000);
                    samples[f * 2 + c] = (short)(v >> 8);
                }
            frame = new(Protocol.Playback, Protocol.Ima, 2, 32_000, sequence++, ImaAdpcm.Encode(samples, 2));
        }
        try { await bridge.SendAsync(frame); } catch { }
    }

    private void ShowFromTray() { Show(); WindowState = FormWindowState.Normal; Activate(); }

    /// <summary>Ctrl+W closes, because a window without a title bar still has to be closable.</summary>
    protected override bool ProcessCmdKey(ref Message message, Keys keyData)
    {
        if (keyData == (Keys.Control | Keys.W)) { Hide(); return true; }
        return base.ProcessCmdKey(ref message, keyData);
    }

    [DllImport("user32.dll")]
    private static extern void ReleaseCapture();

    [DllImport("user32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr SendMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);

    private void EnableDragging(Control control)
    {
        control.MouseDown += (_, e) =>
        {
            if (e.Button != MouseButtons.Left) return;
            // Hand the drag to the window manager rather than tracking it here, so it behaves the
            // way a title bar does — snapping and all.
            ReleaseCapture();
            SendMessage(Handle, 0xA1, 2, IntPtr.Zero);
        };
    }

    protected override void Dispose(bool disposing)
    {
        if (disposing) { tray.Dispose(); capture.Dispose(); micOutput.Dispose(); bridge.DisposeAsync().AsTask().GetAwaiter().GetResult(); }
        base.Dispose(disposing);
    }
}
