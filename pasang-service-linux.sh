#!/usr/bin/env bash
# =============================================================================
#  Pasang aplikasi sebagai layanan systemd (otomatis hidup saat server menyala)
# =============================================================================
#  Jalankan sekali dari folder project:
#      ./pasang-service-linux.sh
#
#  Skrip akan meminta password sudo. Setelah selesai:
#      sudo systemctl status dapodik-smp      # cek status
#      sudo systemctl restart dapodik-smp     # restart
#      sudo systemctl stop dapodik-smp        # hentikan
#      sudo journalctl -u dapodik-smp -f      # lihat log langsung
#
#  Menghapus layanan:
#      sudo systemctl disable --now dapodik-smp
#      sudo rm /etc/systemd/system/dapodik-smp.service
#      sudo systemctl daemon-reload
# =============================================================================
set -euo pipefail

SELF="$(readlink -f "$0")"
APP_DIR="$(cd "$(dirname "$SELF")" && pwd)"
SERVICE_NAME="${SERVICE_NAME:-dapodik-smp}"
UNIT_PATH="/etc/systemd/system/$SERVICE_NAME.service"
PORT="${PORT:-8080}"

# Jalankan ulang sebagai root bila perlu (password sudo diminta sekali)
if [ "$(id -u)" -ne 0 ]; then
  exec sudo --preserve-env=PORT,SERVICE_NAME -- "$SELF" "$@"
fi

RUN_USER="${SUDO_USER:-root}"
if [ "$RUN_USER" = "root" ] && [ -n "${SUDO_USER:-}" ]; then
  RUN_USER="$SUDO_USER"
fi
RUN_HOME="$(getent passwd "$RUN_USER" | cut -d: -f6)"
[ -n "$RUN_HOME" ] || RUN_HOME="/home/$RUN_USER"

[ -x "$APP_DIR/jalankan-linux.sh" ] || chmod +x "$APP_DIR/jalankan-linux.sh"

echo "==> Memasang layanan $SERVICE_NAME"
echo "    Folder aplikasi : $APP_DIR"
echo "    Dijalankan oleh : $RUN_USER (HOME=$RUN_HOME)"
echo "    Port            : $PORT"

cat > "$UNIT_PATH" <<EOF
[Unit]
Description=Dapodik SMP Unggulan Idaman Banjarbaru
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$APP_DIR
Environment=HOME=$RUN_HOME
Environment=PORT=$PORT
ExecStart=$APP_DIR/jalankan-linux.sh
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now "$SERVICE_NAME"

sleep 3
systemctl status "$SERVICE_NAME" --no-pager || true

echo
echo "==> Selesai. Aplikasi berjalan di http://localhost:$PORT"
echo "    Log: sudo journalctl -u $SERVICE_NAME -f"
