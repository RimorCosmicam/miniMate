#include <aspl/Driver.hpp>

#include <CoreAudio/AudioServerPlugIn.h>
#include <arpa/inet.h>
#include <array>
#include <atomic>
#include <cstring>
#include <memory>
#include <netinet/in.h>
#include <sys/socket.h>
#include <unistd.h>

namespace {

constexpr UInt32 SampleRate = 48000;
constexpr UInt32 SpeakerChannels = 2;
constexpr UInt32 MicrophoneChannels = 1;
constexpr uint16_t SpeakerPort = 42310;
constexpr uint16_t MicrophonePort = 42311;

int makeConnectedSocket(uint16_t port)
{
    const int fd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
    if (fd < 0) return -1;
    sockaddr_in address {};
    address.sin_family = AF_INET;
    address.sin_port = htons(port);
    address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
    if (connect(fd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) != 0) {
        close(fd);
        return -1;
    }
    return fd;
}

class SpeakerHandler final : public aspl::ControlRequestHandler, public aspl::IORequestHandler
{
public:
    OSStatus OnStartIO() override
    {
        socket_ = makeConnectedSocket(SpeakerPort);
        return socket_ < 0 ? kAudioHardwareUnspecifiedError : kAudioHardwareNoError;
    }

    void OnStopIO() override
    {
        if (socket_ >= 0) close(socket_);
        socket_ = -1;
    }

    void OnWriteMixedOutput(const std::shared_ptr<aspl::Stream>&, Float64, Float64,
        const void* bytes, UInt32 bytesCount) override
    {
        const auto* cursor = static_cast<const UInt8*>(bytes);
        while (socket_ >= 0 && bytesCount > 0) {
            const UInt32 packetSize = std::min<UInt32>(bytesCount, 1200);
            send(socket_, cursor, packetSize, MSG_DONTWAIT);
            cursor += packetSize;
            bytesCount -= packetSize;
        }
    }

private:
    int socket_ = -1;
};

class MicrophoneHandler final : public aspl::ControlRequestHandler, public aspl::IORequestHandler
{
public:
    OSStatus OnStartIO() override
    {
        socket_ = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
        if (socket_ < 0) return kAudioHardwareUnspecifiedError;
        sockaddr_in address {};
        address.sin_family = AF_INET;
        address.sin_port = htons(MicrophonePort);
        address.sin_addr.s_addr = htonl(INADDR_LOOPBACK);
        if (bind(socket_, reinterpret_cast<sockaddr*>(&address), sizeof(address)) != 0) {
            close(socket_);
            socket_ = -1;
            return kAudioHardwareUnspecifiedError;
        }
        return kAudioHardwareNoError;
    }

    void OnStopIO() override
    {
        if (socket_ >= 0) close(socket_);
        socket_ = -1;
        read_ = write_ = 0;
    }

    void OnReadClientInput(const std::shared_ptr<aspl::Client>&,
        const std::shared_ptr<aspl::Stream>&, Float64, Float64, void* bytes,
        UInt32 bytesCount) override
    {
        std::array<UInt8, 4096> packet {};
        while (socket_ >= 0) {
            const auto received = recv(socket_, packet.data(), packet.size(), MSG_DONTWAIT);
            if (received <= 0) break;
            push(packet.data(), static_cast<size_t>(received));
        }

        auto* destination = static_cast<UInt8*>(bytes);
        size_t copied = 0;
        while (copied < bytesCount && read_ != write_) {
            destination[copied++] = ring_[read_];
            read_ = (read_ + 1) % ring_.size();
        }
        if (copied < bytesCount) std::memset(destination + copied, 0, bytesCount - copied);
    }

private:
    void push(const UInt8* bytes, size_t count)
    {
        for (size_t index = 0; index < count; ++index) {
            const size_t next = (write_ + 1) % ring_.size();
            if (next == read_) read_ = (read_ + 1) % ring_.size();
            ring_[write_] = bytes[index];
            write_ = next;
        }
    }

    int socket_ = -1;
    std::array<UInt8, SampleRate * sizeof(SInt16)> ring_ {};
    size_t read_ = 0;
    size_t write_ = 0;
};

std::shared_ptr<aspl::Driver> createDriver()
{
    auto context = std::make_shared<aspl::Context>();
    auto plugin = std::make_shared<aspl::Plugin>(context);

    aspl::DeviceParameters speakerParameters;
    speakerParameters.Name = "MiniMate Speaker";
    speakerParameters.Manufacturer = "MiniMate";
    speakerParameters.DeviceUID = "com.minimate.audio.speaker";
    speakerParameters.ModelUID = "com.minimate.audio.virtual-output";
    speakerParameters.SampleRate = SampleRate;
    speakerParameters.ChannelCount = SpeakerChannels;
    speakerParameters.EnableMixing = true;
    auto speaker = std::make_shared<aspl::Device>(context, speakerParameters);
    speaker->AddStreamWithControlsAsync(aspl::Direction::Output);
    auto speakerHandler = std::make_shared<SpeakerHandler>();
    speaker->SetControlHandler(speakerHandler);
    speaker->SetIOHandler(speakerHandler);
    plugin->AddDevice(speaker);

    aspl::DeviceParameters microphoneParameters;
    microphoneParameters.Name = "MiniMate Microphone";
    microphoneParameters.Manufacturer = "MiniMate";
    microphoneParameters.DeviceUID = "com.minimate.audio.microphone";
    microphoneParameters.ModelUID = "com.minimate.audio.virtual-input";
    microphoneParameters.SampleRate = SampleRate;
    microphoneParameters.ChannelCount = MicrophoneChannels;
    microphoneParameters.CanBeDefaultForSystemSounds = false;
    auto microphone = std::make_shared<aspl::Device>(context, microphoneParameters);
    microphone->AddStreamWithControlsAsync(aspl::Direction::Input);
    auto microphoneHandler = std::make_shared<MicrophoneHandler>();
    microphone->SetControlHandler(microphoneHandler);
    microphone->SetIOHandler(microphoneHandler);
    plugin->AddDevice(microphone);

    return std::make_shared<aspl::Driver>(context, plugin);
}

} // namespace

extern "C" void* MiniMateAudioEntryPoint(CFAllocatorRef, CFUUIDRef typeUUID)
{
    if (!CFEqual(typeUUID, kAudioServerPlugInTypeUUID)) return nullptr;
    static std::shared_ptr<aspl::Driver> driver = createDriver();
    return driver->GetReference();
}

