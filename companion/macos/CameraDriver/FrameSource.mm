#import <AppKit/AppKit.h>
#import <CoreVideo/CoreVideo.h>
#import <ImageIO/ImageIO.h>
#import <objc/runtime.h>
#import "Stream.h"

static const int MMWidth = 1280;
static const int MMHeight = 720;

@interface Stream (MiniMateFrameSource)
- (CVPixelBufferRef)mm_createPixelBufferWithTestAnimation;
@end

@implementation Stream (MiniMateFrameSource)

+ (void)load {
    Method original = class_getInstanceMethod(self, @selector(createPixelBufferWithTestAnimation));
    Method replacement = class_getInstanceMethod(self, @selector(mm_createPixelBufferWithTestAnimation));
    if (original && replacement) method_exchangeImplementations(original, replacement);
}

- (CVPixelBufferRef)mm_createPixelBufferWithTestAnimation {
    NSDictionary *options = @{
        (__bridge NSString *)kCVPixelBufferCGImageCompatibilityKey: @YES,
        (__bridge NSString *)kCVPixelBufferCGBitmapContextCompatibilityKey: @YES
    };
    CVPixelBufferRef pixelBuffer = NULL;
    CVReturn status = CVPixelBufferCreate(kCFAllocatorDefault, MMWidth, MMHeight,
                                           kCVPixelFormatType_32ARGB,
                                           (__bridge CFDictionaryRef)options, &pixelBuffer);
    if (status != kCVReturnSuccess || !pixelBuffer) return NULL;

    CVPixelBufferLockBaseAddress(pixelBuffer, 0);
    CGColorSpaceRef colorSpace = CGColorSpaceCreateDeviceRGB();
    CGContextRef context = CGBitmapContextCreate(
        CVPixelBufferGetBaseAddress(pixelBuffer), MMWidth, MMHeight, 8,
        CVPixelBufferGetBytesPerRow(pixelBuffer), colorSpace,
        kCGImageAlphaPremultipliedFirst | kCGBitmapByteOrder32Big
    );
    CGContextSetRGBFillColor(context, 0, 0, 0, 1);
    CGContextFillRect(context, CGRectMake(0, 0, MMWidth, MMHeight));

    NSURL *frameURL = [NSURL fileURLWithPath:@"/tmp/minimate-camera.jpg"];
    CGImageSourceRef source = CGImageSourceCreateWithURL((__bridge CFURLRef)frameURL, NULL);
    CGImageRef image = source ? CGImageSourceCreateImageAtIndex(source, 0, NULL) : NULL;
    if (image) {
        CGFloat imageWidth = CGImageGetWidth(image);
        CGFloat imageHeight = CGImageGetHeight(image);
        CGFloat scale = MAX(MMWidth / imageWidth, MMHeight / imageHeight);
        CGFloat width = imageWidth * scale;
        CGFloat height = imageHeight * scale;
        CGContextSaveGState(context);
        CGContextTranslateCTM(context, 0, MMHeight);
        CGContextScaleCTM(context, 1, -1);
        CGContextSetInterpolationQuality(context, kCGInterpolationHigh);
        CGContextDrawImage(context, CGRectMake((MMWidth - width) / 2, (MMHeight - height) / 2, width, height), image);
        CGContextRestoreGState(context);
        CGImageRelease(image);
    } else {
        NSDictionary *attributes = @{ NSFontAttributeName: [NSFont systemFontOfSize:42 weight:NSFontWeightSemibold], NSForegroundColorAttributeName: NSColor.whiteColor };
        NSGraphicsContext *graphics = [NSGraphicsContext graphicsContextWithCGContext:context flipped:NO];
        [NSGraphicsContext saveGraphicsState];
        NSGraphicsContext.currentContext = graphics;
        [@"MiniMate Camera" drawAtPoint:NSMakePoint(42, 50) withAttributes:attributes];
        [NSGraphicsContext restoreGraphicsState];
    }
    if (source) CFRelease(source);
    CGContextRelease(context);
    CGColorSpaceRelease(colorSpace);
    CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    return pixelBuffer;
}

@end
