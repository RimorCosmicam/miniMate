#!/bin/zsh
set -euo pipefail

root_dir="${0:A:h}"
cd "$root_dir"

driver_build="$root_dir/.build/driver"
cmake -S Driver -B "$driver_build" -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=OFF -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64"
cmake --build "$driver_build" --config Release --parallel
codesign --force --sign - "$driver_build/MiniMateAudio.driver"

# The legacy DAL is the functional private-distribution fallback for ad-hoc
# builds. Pin the small MIT foundation, then layer MiniMate's frame source on it.
dal_repo="$root_dir/.build/coremediaio-dal"
dal_commit="d7d24bc801f07303ac3367c2791fbf13f573cc7c"
if [[ ! -d "$dal_repo/.git" ]]; then
  git clone --filter=blob:none https://github.com/johnboiles/coremediaio-dal-minimal-example.git "$dal_repo"
fi
git -C "$dal_repo" fetch --depth 1 origin "$dal_commit"
git -C "$dal_repo" checkout --force "$dal_commit"
dal_source="$dal_repo/CMIOMinimalSample"
/usr/bin/sed -i '' \
  -e 's/CMIOMinimalSample Device/MiniMate Camera/g' \
  -e 's/CMIOMinimalSample Stream/MiniMate Camera Stream/g' \
  -e 's/CMIOMinimalSample Plugin/MiniMate Camera Plugin/g' \
  -e 's/CMIO Simple Device/com.minimate.camera.device/g' \
  -e 's/CMIO Simple Model/com.minimate.camera.model/g' \
  -e 's/kCMVideoCodecType_422YpCbCr8/kCVPixelFormatType_32ARGB/g' \
  "$dal_source/Device.mm" "$dal_source/PlugIn.mm" "$dal_source/Stream.mm"

camera_build="$root_dir/.build/camera-driver"
cmake -S CameraDriver -B "$camera_build" -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64" -DDAL_SOURCE="$dal_source"
cmake --build "$camera_build" --config Release --parallel
codesign --force --sign - "$camera_build/MiniMateCamera.plugin"

swift build -c release

app_dir="$root_dir/dist/MiniMate Audio.app"
contents="$app_dir/Contents"
rm -rf "$app_dir"
mkdir -p "$contents/MacOS" "$contents/Resources"
cp .build/release/MiniMateAudio "$contents/MacOS/MiniMateAudio"
cp Info.plist "$contents/Info.plist"
cp -R .build/release/MiniMateAudio_MiniMateAudio.bundle "$contents/Resources/"
cp -R "$driver_build/MiniMateAudio.driver" "$contents/Resources/MiniMateAudio.driver"
cp -R "$camera_build/MiniMateCamera.plugin" "$contents/Resources/MiniMateCamera.plugin"
cp CameraDriver/LICENSE.coremediaio-dal-minimal-example "$contents/Resources/"

iconset="$root_dir/.build/AppIcon.iconset"
rm -rf "$iconset"
mkdir -p "$iconset"
for size in 16 32 128 256 512; do
  sips -z "$size" "$size" Sources/Resources/Mini.png --out "$iconset/icon_${size}x${size}.png" >/dev/null
  double=$((size * 2))
  sips -z "$double" "$double" Sources/Resources/Mini.png --out "$iconset/icon_${size}x${size}@2x.png" >/dev/null
done
iconutil -c icns "$iconset" -o "$contents/Resources/AppIcon.icns"
codesign --force --deep --sign - "$app_dir"
/usr/bin/plutil -lint "$contents/Info.plist"
/usr/bin/codesign --verify --deep --strict "$app_dir"
test -x "$contents/MacOS/MiniMateAudio"
test -x "$contents/Resources/MiniMateAudio.driver/Contents/MacOS/MiniMateAudio"
test -x "$contents/Resources/MiniMateCamera.plugin/Contents/MacOS/MiniMateCamera"
echo "$app_dir"
