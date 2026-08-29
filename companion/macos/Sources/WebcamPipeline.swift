import CoreImage
import CoreImage.CIFilterBuiltins
import Foundation

/// Processes the phone camera independently of the companion window and
/// atomically publishes one latest frame for the CoreMediaIO camera plug-in.
final class WebcamPipeline: @unchecked Sendable {
    static let frameURL = URL(fileURLWithPath: "/tmp/minimate-camera.jpg")
    private let queue = DispatchQueue(label: "MiniMate.Webcam", qos: .userInteractive)
    private let context = CIContext(options: [.cacheIntermediates: true])
    private var filters: [String] = []
    private var intensity = 1.0
    private var mirror = false
    private var enabled = false

    func configure(_ payload: Data) {
        queue.async { [self] in
            guard let object = try? JSONSerialization.jsonObject(with: payload) as? [String: Any] else { return }
            enabled = object["enabled"] as? Bool ?? false
            mirror = object["mirror"] as? Bool ?? false
            intensity = (object["intensity"] as? Double ?? 1).clamped(to: 0...1)
            filters = object["filters"] as? [String] ?? []
        }
    }

    func consume(jpeg: Data) {
        queue.async { [self] in
            guard enabled, var image = CIImage(data: jpeg) else { return }
            let original = image
            if mirror {
                image = image.transformed(by: CGAffineTransform(translationX: image.extent.width, y: 0).scaledBy(x: -1, y: 1))
            }
            for name in filters { image = apply(name, to: image).cropped(to: image.extent) }
            if intensity < 0.999, !filters.isEmpty,
               let dissolve = CIFilter(name: "CIDissolveTransition") {
                dissolve.setValue(image, forKey: kCIInputImageKey)
                dissolve.setValue(original, forKey: kCIInputBackgroundImageKey)
                dissolve.setValue(intensity, forKey: kCIInputTimeKey)
                image = dissolve.outputImage ?? image
            }
            guard let color = CGColorSpace(name: CGColorSpace.sRGB),
                  let data = context.jpegRepresentation(of: image, colorSpace: color, options: [:])
            else { return }
            try? data.write(to: Self.frameURL, options: .atomic)
        }
    }

    private func apply(_ name: String, to input: CIImage) -> CIImage {
        let extent = input.extent
        let center = CIVector(x: extent.midX, y: extent.midY)
        func filtered(_ filterName: String, _ values: [String: Any] = [:]) -> CIImage {
            guard let filter = CIFilter(name: filterName) else { return input }
            filter.setValue(input, forKey: kCIInputImageKey)
            values.forEach { filter.setValue($1, forKey: $0) }
            return filter.outputImage ?? input
        }
        switch name {
        case "CHROMATIC":
            let red = filtered("CIColorMatrix", ["inputRVector": CIVector(x: 1, y: 0, z: 0, w: 0), "inputGVector": CIVector(x: 0, y: 0, z: 0, w: 0), "inputBVector": CIVector(x: 0, y: 0, z: 0, w: 0)])
                .transformed(by: .init(translationX: 5, y: 0))
            let cyan = filtered("CIColorMatrix", ["inputRVector": CIVector(x: 0, y: 0, z: 0, w: 0)])
                .transformed(by: .init(translationX: -4, y: 0))
            return red.applyingFilter("CIAdditionCompositing", parameters: [kCIInputBackgroundImageKey: cyan])
        case "CRT": return filtered("CILineScreen", [kCIInputCenterKey: center, kCIInputWidthKey: 2.2, kCIInputSharpnessKey: 0.35])
        case "VHS": return filtered("CIColorControls", [kCIInputSaturationKey: 0.82, kCIInputContrastKey: 1.13]).applyingFilter("CIGaussianBlur", parameters: [kCIInputRadiusKey: 0.7])
        case "PIXELATE": return filtered("CIPixellate", [kCIInputCenterKey: center, kCIInputScaleKey: 12])
        case "DREAM_BLOOM": return filtered("CIBloom", [kCIInputRadiusKey: 14, kCIInputIntensityKey: 0.85])
        case "MONO_INK": return filtered("CIPhotoEffectNoir")
        case "KALEIDOSCOPE": return filtered("CIKaleidoscope", [kCIInputCenterKey: center, "inputCount": 8, "inputAngle": 0.2])
        case "FISHEYE": return filtered("CIBumpDistortion", [kCIInputCenterKey: center, kCIInputRadiusKey: min(extent.width, extent.height) * 0.7, kCIInputScaleKey: -0.42])
        case "HALFTONE": return filtered("CIDotScreen", [kCIInputCenterKey: center, kCIInputWidthKey: 7, kCIInputSharpnessKey: 0.65])
        case "THERMAL": return filtered("CIFalseColor", ["inputColor0": CIColor(red: 0.03, green: 0, blue: 0.2), "inputColor1": CIColor(red: 1, green: 0.93, blue: 0.08)])
        case "NEGATIVE": return filtered("CIColorInvert")
        case "POSTERIZE": return filtered("CIColorPosterize", ["inputLevels": 6])
        case "FILM_GRAIN": return filtered("CIPhotoEffectProcess").applyingFilter("CINoiseReduction", parameters: ["inputNoiseLevel": 0.035, "inputSharpness": 0.55])
        case "MIRROR_PRISM": return filtered("CITriangleKaleidoscope", ["inputPoint": center, "inputSize": min(extent.width, extent.height) * 0.55, "inputRotation": 0.7, "inputDecay": 0.86])
        case "LIQUID_GLASS": return filtered("CIBumpDistortion", [kCIInputCenterKey: center, kCIInputRadiusKey: min(extent.width, extent.height) * 0.34, kCIInputScaleKey: 0.32])
        case "NIGHT_VISION": return filtered("CIPhotoEffectMono").applyingFilter("CIFalseColor", parameters: ["inputColor0": CIColor.black, "inputColor1": CIColor(red: 0.36, green: 1, blue: 0.38)]).applyingFilter("CIBloom", parameters: [kCIInputRadiusKey: 5, kCIInputIntensityKey: 0.55])
        default: return input
        }
    }
}

private extension Comparable {
    func clamped(to range: ClosedRange<Self>) -> Self { min(max(self, range.lowerBound), range.upperBound) }
}
