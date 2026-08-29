#!/bin/zsh
set -euo pipefail

root_dir="${0:A:h}"
app_path="$root_dir/dist/MiniMate Audio.app"
package_root="$root_dir/dist/package-root"
package_path="$root_dir/dist/MiniMate-Audio-macOS.pkg"
component_plist="$root_dir/dist/components.plist"

if [[ ! -d "$app_path" ]]; then
  echo "Build the application with ./build-app.sh first." >&2
  exit 1
fi

rm -rf "$package_root" "$package_path" "$component_plist"
mkdir -p "$package_root/Applications" "$package_root/Library/Audio/Plug-Ins/HAL" "$package_root/Library/CoreMediaIO/Plug-Ins/DAL"
/usr/bin/ditto "$app_path" "$package_root/Applications/MiniMate Audio.app"
/usr/bin/ditto \
  "$app_path/Contents/Resources/MiniMateAudio.driver" \
  "$package_root/Library/Audio/Plug-Ins/HAL/MiniMateAudio.driver"
/usr/bin/ditto \
  "$app_path/Contents/Resources/MiniMateCamera.plugin" \
  "$package_root/Library/CoreMediaIO/Plug-Ins/DAL/MiniMateCamera.plugin"

# PackageKit otherwise searches the whole Mac for matching bundle identifiers and
# may "relocate" MiniMate back into an extracted download/build directory. Pin every
# component to its payload path so the app always lands in /Applications and the
# driver always lands in /Library/Audio/Plug-Ins/HAL.
/usr/bin/pkgbuild --analyze --root "$package_root" "$component_plist"
component_index=0
while /usr/libexec/PlistBuddy -c "Print :$component_index" "$component_plist" >/dev/null 2>&1; do
  /usr/libexec/PlistBuddy -c "Delete :$component_index:BundleIsRelocatable" "$component_plist" >/dev/null 2>&1 || true
  /usr/libexec/PlistBuddy -c "Add :$component_index:BundleIsRelocatable bool false" "$component_plist"
  component_index=$((component_index + 1))
done

/usr/bin/pkgbuild \
  --root "$package_root" \
  --component-plist "$component_plist" \
  --scripts "$root_dir/Installer" \
  --identifier com.minimate.audio.installer \
  --version 1.0.0 \
  --install-location / \
  --ownership recommended \
  "$package_path"

# Refuse to publish a cosmetically valid but empty/broken installer.
payload=$(/usr/sbin/pkgutil --payload-files "$package_path")
grep -Fq "./Applications/MiniMate Audio.app/Contents/MacOS/MiniMateAudio" <<< "$payload"
grep -Fq "./Library/Audio/Plug-Ins/HAL/MiniMateAudio.driver/Contents/MacOS/MiniMateAudio" <<< "$payload"
grep -Fq "./Library/CoreMediaIO/Plug-Ins/DAL/MiniMateCamera.plugin/Contents/MacOS/MiniMateCamera" <<< "$payload"

echo "$package_path"
