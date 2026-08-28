#!/bin/zsh
set -euo pipefail

root_dir="${0:A:h}"
app_path="$root_dir/dist/MiniMate Audio.app"
package_root="$root_dir/dist/package-root"
package_path="$root_dir/dist/MiniMate-Audio-macOS.pkg"

if [[ ! -d "$app_path" ]]; then
  echo "Build the application with ./build-app.sh first." >&2
  exit 1
fi

rm -rf "$package_root" "$package_path"
mkdir -p "$package_root/Applications" "$package_root/Library/Audio/Plug-Ins/HAL"
/usr/bin/ditto "$app_path" "$package_root/Applications/MiniMate Audio.app"
/usr/bin/ditto \
  "$app_path/Contents/Resources/MiniMateAudio.driver" \
  "$package_root/Library/Audio/Plug-Ins/HAL/MiniMateAudio.driver"

/usr/bin/pkgbuild \
  --root "$package_root" \
  --scripts "$root_dir/Installer" \
  --identifier com.minimate.audio.installer \
  --version 1.0.0 \
  --install-location / \
  --ownership recommended \
  "$package_path"

echo "$package_path"

