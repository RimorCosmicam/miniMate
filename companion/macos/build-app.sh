#!/bin/zsh
set -euo pipefail

root_dir="${0:A:h}"
cd "$root_dir"
swift build -c release

app_dir="$root_dir/dist/MiniMate Audio.app"
contents="$app_dir/Contents"
rm -rf "$app_dir"
mkdir -p "$contents/MacOS" "$contents/Resources"
cp .build/release/MiniMateAudio "$contents/MacOS/MiniMateAudio"
cp Info.plist "$contents/Info.plist"
cp -R .build/release/MiniMateAudio_MiniMateAudio.bundle "$contents/Resources/"

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
