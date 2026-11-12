#!/usr/bin/env bash
# Run an install command and tap MIUI's "Install via USB" confirmation while it runs.
# MIUI asks on every adb install (com.miui.securitycenter AdbInstallActivity) and cancels it
# with INSTALL_FAILED_USER_RESTRICTED after a 10 s countdown.
#
#   ANDROID_SERIAL=64e3f5b9 scripts/mi-autotap.sh adb install -r -t app.apk
#   ANDROID_SERIAL=64e3f5b9 scripts/mi-autotap.sh ./gradlew :androidCMP:installDebug
set -u
S="${ANDROID_SERIAL:?set ANDROID_SERIAL to the phone serial}"
[ $# -gt 0 ] || { echo "usage: $0 <install command...>" >&2; exit 2; }

"$@" &
cmd=$!

while kill -0 "$cmd" 2>/dev/null; do
  if adb -s "$S" shell dumpsys window 2>/dev/null | grep mCurrentFocus | grep -q AdbInstallActivity; then
    # A dump takes ~5 s; the Install button is android:id/button2 (text "Install").
    node=$(MSYS_NO_PATHCONV=1 adb -s "$S" exec-out uiautomator dump /dev/tty 2>/dev/null |
      grep -o '<node [^>]*>' | grep -E 'resource-id="android:id/button2"|text="Install"' | grep -v Deny | head -1)
    b=$(printf '%s' "$node" | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
    if [ -n "$b" ]; then
      read -r x1 y1 x2 y2 <<< "$b"
      echo "mi-autotap: tapping Install at $(( (x1 + x2) / 2 )),$(( (y1 + y2) / 2 ))" >&2
      adb -s "$S" shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
      sleep 2
    fi
  fi
  sleep 0.5
done

wait "$cmd"
