#!/usr/bin/env bash
set -euo pipefail
VERSION="${1:-0.1.0}"
ARCH="all"
PKG="seamless-clipboard"
ROOT="build/${PKG}_${VERSION}_${ARCH}"
rm -rf build
mkdir -p "$ROOT/DEBIAN" "$ROOT/usr/lib/seamless-clipboard" "$ROOT/usr/lib/systemd/user" "$ROOT/usr/bin" "$ROOT/etc/xdg/autostart"
cat > "$ROOT/DEBIAN/control" <<EOF
Package: $PKG
Version: $VERSION
Section: utils
Priority: optional
Architecture: $ARCH
Maintainer: abe618-ops
Depends: python3 (>= 3.10), python3-zeroconf
Recommends: wl-clipboard | xclip
Description: Seamless Android-Linux clipboard sync daemon
 Automatically advertises on the local network using mDNS and synchronizes
 plain-text clipboard content with paired Android clients. Designed for
 one-time pairing and automatic reconnection on the same Wi-Fi/LAN.
EOF
install -m 0755 seamclipd.py "$ROOT/usr/lib/seamless-clipboard/seamclipd.py"
cat > "$ROOT/usr/lib/systemd/user/seamless-clipboard.service" <<'EOF'
[Unit]
Description=Seamless Clipboard Sync
After=graphical-session.target network-online.target
Wants=network-online.target

[Service]
Type=simple
ExecStart=/usr/bin/python3 /usr/lib/seamless-clipboard/seamclipd.py
Restart=always
RestartSec=2

[Install]
WantedBy=default.target
EOF
cat > "$ROOT/usr/bin/seamless-clipboard-start" <<'EOF'
#!/usr/bin/env bash
systemctl --user daemon-reload || true
systemctl --user enable --now seamless-clipboard.service || true
EOF
chmod 0755 "$ROOT/usr/bin/seamless-clipboard-start"
cat > "$ROOT/etc/xdg/autostart/seamless-clipboard.desktop" <<'EOF'
[Desktop Entry]
Type=Application
Name=Seamless Clipboard
Comment=Start Seamless Clipboard Sync automatically
Exec=/usr/bin/seamless-clipboard-start
Terminal=false
X-GNOME-Autostart-enabled=true
NoDisplay=true
EOF
dpkg-deb --root-owner-group --build "$ROOT"
mv "build/${PKG}_${VERSION}_${ARCH}.deb" "${PKG}_${VERSION}_${ARCH}.deb"
dpkg-deb -I "${PKG}_${VERSION}_${ARCH}.deb"
sha256sum "${PKG}_${VERSION}_${ARCH}.deb"
