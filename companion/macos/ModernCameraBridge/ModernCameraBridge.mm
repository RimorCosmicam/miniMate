#import "ModernCameraBridge.h"

#import <Foundation/Foundation.h>
#import <CoreFoundation/CoreFoundation.h>
#import <CoreGraphics/CoreGraphics.h>
#import <CoreMedia/CoreMedia.h>
#import <CoreMediaIO/CoreMediaIO.h>
#import <CoreVideo/CoreVideo.h>
#import <ImageIO/ImageIO.h>
#import <os/lock.h>

static const CFStringRef MMOBSDeviceUID = CFSTR("7626645E-4425-469E-9D8B-97E0FA59AC75");
static const int32_t MMWidth = 1920;
static const int32_t MMHeight = 1080;

static os_unfair_lock MMLock = OS_UNFAIR_LOCK_INIT;
static CMIODeviceID MMDevice = kCMIOObjectUnknown;
static CMIOStreamID MMStream = kCMIOObjectUnknown;
static CMSimpleQueueRef MMQueue = NULL;
static CVPixelBufferPoolRef MMPool = NULL;
static CMFormatDescriptionRef MMFormat = NULL;
static bool MMStarted = false;

static void MMQueueChanged(CMIOStreamID streamID, void *token, void *refCon) {
    (void)streamID; (void)token; (void)refCon;
}

static bool MMCopyStringProperty(CMIOObjectID object,
                                 CMIOObjectPropertySelector selector,
                                 CFStringRef *result) {
    CMIOObjectPropertyAddress address = {
        selector, kCMIOObjectPropertyScopeGlobal, kCMIOObjectPropertyElementMain
    };
    UInt32 size = sizeof(CFStringRef);
    UInt32 used = 0;
    CFStringRef value = NULL;
    OSStatus status = CMIOObjectGetPropertyData(object, &address, 0, NULL,
                                                size, &used, &value);
    if (status != noErr || !value) return false;
    *result = value;
    return true;
}

static bool MMFindOBSDevice(CMIODeviceID *deviceOut, CMIOStreamID *streamOut) {
    CMIOObjectPropertyAddress address = {
        kCMIOHardwarePropertyDevices,
        kCMIOObjectPropertyScopeGlobal,
        kCMIOObjectPropertyElementMain
    };
    UInt32 size = 0;
    if (CMIOObjectGetPropertyDataSize(kCMIOObjectSystemObject, &address, 0, NULL, &size) != noErr ||
        size < sizeof(CMIODeviceID)) return false;

    size_t count = size / sizeof(CMIODeviceID);
    CMIODeviceID *devices = static_cast<CMIODeviceID *>(calloc(count, sizeof(CMIODeviceID)));
    if (!devices) return false;
    UInt32 used = 0;
    bool found = false;
    if (CMIOObjectGetPropertyData(kCMIOObjectSystemObject, &address, 0, NULL,
                                  size, &used, devices) == noErr) {
        count = used / sizeof(CMIODeviceID);
        for (size_t index = 0; index < count && !found; index++) {
            CFStringRef uid = NULL;
            if (!MMCopyStringProperty(devices[index], kCMIODevicePropertyDeviceUID, &uid)) continue;
            bool matches = CFEqual(uid, MMOBSDeviceUID);
            CFRelease(uid);
            if (!matches) continue;

            address.mSelector = kCMIODevicePropertyStreams;
            address.mScope = kCMIODevicePropertyScopeOutput;
            size = 0;
            if (CMIOObjectGetPropertyDataSize(devices[index], &address, 0, NULL, &size) != noErr ||
                size < sizeof(CMIOStreamID)) {
                // OBS 30 exposes source then sink in global scope. Retain this fallback
                // for that release while preferring the explicit output scope above.
                address.mScope = kCMIOObjectPropertyScopeGlobal;
                if (CMIOObjectGetPropertyDataSize(devices[index], &address, 0, NULL, &size) != noErr ||
                    size < 2 * sizeof(CMIOStreamID)) continue;
            }
            size_t streamCount = size / sizeof(CMIOStreamID);
            CMIOStreamID *streams = static_cast<CMIOStreamID *>(calloc(streamCount, sizeof(CMIOStreamID)));
            if (!streams) continue;
            UInt32 streamUsed = 0;
            if (CMIOObjectGetPropertyData(devices[index], &address, 0, NULL, size,
                                          &streamUsed, streams) == noErr) {
                *deviceOut = devices[index];
                *streamOut = address.mScope == kCMIODevicePropertyScopeOutput
                    ? streams[0]
                    : streams[streamCount - 1];
                found = true;
            }
            free(streams);
        }
    }
    free(devices);
    return found;
}

static bool MMStartLocked(void) {
    if (MMStarted) return true;
    if (!MMFindOBSDevice(&MMDevice, &MMStream)) return false;

    if (CMIOStreamCopyBufferQueue(MMStream, MMQueueChanged, NULL, &MMQueue) != noErr || !MMQueue)
        return false;

    NSDictionary *attributes = @{
        (__bridge NSString *)kCVPixelBufferWidthKey: @(MMWidth),
        (__bridge NSString *)kCVPixelBufferHeightKey: @(MMHeight),
        (__bridge NSString *)kCVPixelBufferPixelFormatTypeKey: @(kCVPixelFormatType_32BGRA),
        (__bridge NSString *)kCVPixelBufferIOSurfacePropertiesKey: @{}
    };
    if (CVPixelBufferPoolCreate(kCFAllocatorDefault, NULL,
                                (__bridge CFDictionaryRef)attributes, &MMPool) != kCVReturnSuccess ||
        !MMPool) return false;
    if (CMVideoFormatDescriptionCreate(kCFAllocatorDefault, kCVPixelFormatType_32BGRA,
                                       MMWidth, MMHeight, NULL, &MMFormat) != noErr || !MMFormat)
        return false;
    if (CMIODeviceStartStream(MMDevice, MMStream) != noErr) return false;
    MMStarted = true;
    return true;
}

bool MMModernCameraAvailable(void) {
    os_unfair_lock_lock(&MMLock);
    CMIODeviceID device = kCMIOObjectUnknown;
    CMIOStreamID stream = kCMIOObjectUnknown;
    bool available = MMStarted || MMFindOBSDevice(&device, &stream);
    os_unfair_lock_unlock(&MMLock);
    return available;
}

bool MMModernCameraSendJPEG(const uint8_t *bytes, size_t length) {
    if (!bytes || length == 0) return false;
    os_unfair_lock_lock(&MMLock);
    bool success = false;
    @autoreleasepool {
        if (MMStartLocked() && CMSimpleQueueGetCount(MMQueue) < CMSimpleQueueGetCapacity(MMQueue)) {
            CFDataRef data = CFDataCreateWithBytesNoCopy(kCFAllocatorDefault, bytes,
                                                          (CFIndex)length, kCFAllocatorNull);
            CGImageSourceRef source = data ? CGImageSourceCreateWithData(data, NULL) : NULL;
            CGImageRef image = source ? CGImageSourceCreateImageAtIndex(source, 0, NULL) : NULL;
            CVPixelBufferRef pixel = NULL;
            if (image && CVPixelBufferPoolCreatePixelBuffer(kCFAllocatorDefault, MMPool, &pixel) == kCVReturnSuccess &&
                pixel) {
                CVPixelBufferLockBaseAddress(pixel, 0);
                CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
                CGContextRef context = CGBitmapContextCreate(CVPixelBufferGetBaseAddress(pixel), MMWidth, MMHeight,
                    8, CVPixelBufferGetBytesPerRow(pixel), colorSpace,
                    kCGImageAlphaNoneSkipFirst | kCGBitmapByteOrder32Little);
                if (context) {
                    CGFloat sourceWidth = CGImageGetWidth(image);
                    CGFloat sourceHeight = CGImageGetHeight(image);
                    CGFloat scale = MAX((CGFloat)MMWidth / sourceWidth, (CGFloat)MMHeight / sourceHeight);
                    CGRect target = CGRectMake((MMWidth - sourceWidth * scale) * 0.5,
                                               (MMHeight - sourceHeight * scale) * 0.5,
                                               sourceWidth * scale, sourceHeight * scale);
                    CGContextSetRGBFillColor(context, 0, 0, 0, 1);
                    CGContextFillRect(context, CGRectMake(0, 0, MMWidth, MMHeight));
                    CGContextSetInterpolationQuality(context, kCGInterpolationHigh);
                    CGContextTranslateCTM(context, 0, MMHeight);
                    CGContextScaleCTM(context, 1, -1);
                    CGContextDrawImage(context, target, image);

                    CMSampleTimingInfo timing = {
                        .duration = kCMTimeInvalid,
                        .presentationTimeStamp = CMClockGetTime(CMClockGetHostTimeClock()),
                        .decodeTimeStamp = kCMTimeInvalid
                    };
                    CMSampleBufferRef sample = NULL;
                    if (CMSampleBufferCreateForImageBuffer(kCFAllocatorDefault, pixel, true, NULL, NULL,
                                                           MMFormat, &timing, &sample) == noErr && sample) {
                        success = CMSimpleQueueEnqueue(MMQueue, sample) == noErr;
                        if (!success) CFRelease(sample);
                    }
                    CGContextRelease(context);
                }
                CGColorSpaceRelease(colorSpace);
                CVPixelBufferUnlockBaseAddress(pixel, 0);
                CVPixelBufferRelease(pixel);
            }
            if (image) CGImageRelease(image);
            if (source) CFRelease(source);
            if (data) CFRelease(data);
        }
    }
    os_unfair_lock_unlock(&MMLock);
    return success;
}

void MMModernCameraStop(void) {
    os_unfair_lock_lock(&MMLock);
    if (MMStarted) CMIODeviceStopStream(MMDevice, MMStream);
    if (MMQueue) CFRelease(MMQueue);
    if (MMPool) CFRelease(MMPool);
    if (MMFormat) CFRelease(MMFormat);
    MMQueue = NULL; MMPool = NULL; MMFormat = NULL;
    MMDevice = kCMIOObjectUnknown; MMStream = kCMIOObjectUnknown;
    MMStarted = false;
    os_unfair_lock_unlock(&MMLock);
}
