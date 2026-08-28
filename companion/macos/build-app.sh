#!/bin/zsh
set -euo pipefail

root_dir="${0:A:h}"
cd "$root_dir"

driver_build="$root_dir/.build/driver"
cmake -S Driver -B "$driver_build" -DCMAKE_BUILD_TYPE=Release -DBUILD_TESTING=OFF -DCMAKE_OSX_ARCHITECTURES="arm64;x86_64"
cmake --build "$driver_build" --config Release --parallel
codesign --force --sign - "$driver_build/MiniMateAudio.driver"

swift build -c release

app_dir="$root_dir/dist/MiniMate Audio.app"
contents="$app_dir/Contents"
rm -rf "$app_dir"
mkdir -p "$contents/MacOS" "$contents/Resources"
cp .build/release/MiniMateAudio "$contents/MacOS/MiniMateAudio"
cp Info.plist "$contents/Info.plist"
cp -R .build/release/MiniMateAudio_MiniMateAudio.bundle "$contents/Resources/"
cp -R "$driver_build/MiniMateAudio.driver" "$contents/Resources/MiniMateAudio.driver"

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
echo "$app_dir"
