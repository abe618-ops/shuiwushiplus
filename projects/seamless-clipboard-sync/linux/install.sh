#!/usr/bin/env bash
set -euo pipefail
BASE="$HOME/.local/share/seamless-clipboard"
mkdir -p "$BASE" "$HOME/.config/systemd/user"
cp "$(dirname "$0")/seamclipd.py" "$BASE/seamclipd.py"
python3 -m venv "$BASE/venv"
"$BASE/venv/bin/pip" install --upgrade pip zeroconf
cat > "$HOME/.config/systemd/user/seamless-clipboard.service" <<EOF
[Unit]
Description=Seamless Clipboard Sync
After=graphical-session.target network-online.target

[Service]
ExecStart=$BASE/venv/bin/python $BASE/seamclipd.py
Restart=always
RestartSec=2
Environment=DISPLAY=:0

[Install]
WantedBy=default.target
EOF
systemctl --user daemon-reload
systemctl --user enable --now seamless-clipboard.service
printf '\nInstalled. The daemon now starts automatically.\nAndroid should discover it on the same Wi-Fi without entering an IP address.\n'
