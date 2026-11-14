#!/usr/bin/env bash
# =============================================================================
#  Menjalankan Aplikasi Dapodik SMP Unggulan Idaman Banjarbaru di Linux
# =============================================================================
#  Default: database lokal H2 (file ./data/dapodik-smp.mv.db) — tanpa PostgreSQL.
#  Untuk PostgreSQL/Neon cukup set SPRING_DATASOURCE_URL (dan DB_USERNAME/DB_PASSWORD).
#
#  Pemakaian:
#    ./jalankan-linux.sh                 # mode H2 lokal, port 8080
#    PORT=9090 ./jalankan-linux.sh       # ganti port
#    ./jalankan-linux.sh --server.port=9090
#
#    # Mode PostgreSQL:
#    SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/kepegawaian_smp' \
#    DB_USERNAME=postgres DB_PASSWORD=postgres ./jalankan-linux.sh
#
#  Hentikan dengan Ctrl+C.
# =============================================================================
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

APP_JAR="target/kepegawaian-smp-2.0.0.jar"
DATA_DIR="${DATA_DIR:-$PWD/data}"
DATA_NAME="${DATA_NAME:-dapodik-smp}"
H2_VERSION="2.2.224"
H2_JAR="$HOME/.m2/repository/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"
H2_URL="https://repo1.maven.org/maven2/com/h2database/h2/$H2_VERSION/h2-$H2_VERSION.jar"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31mERROR:\033[0m %s\n' "$*" >&2; exit 1; }

# --- 1. Cari Java 17+ --------------------------------------------------------
versi_java() {
  "$1" -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1
}

cari_java() {
  local kandidat v
  for kandidat in \
    ${JAVA_HOME:+"$JAVA_HOME/bin/java"} \
    "$HOME/.local/jre17/bin/java" \
    "$HOME"/.local/jdk-17*/bin/java \
    "$HOME"/.sdkman/candidates/java/17*/bin/java \
    /usr/lib/jvm/java-17*/bin/java \
    /usr/lib/jvm/temurin-17*/bin/java \
    /usr/lib/jvm/java-1.17*/bin/java \
    /opt/java-17*/bin/java; do
    [ -x "$kandidat" ] || continue
    v="$(versi_java "$kandidat" || true)"
    if [ -n "$v" ] && [ "$v" -ge 17 ] 2>/dev/null; then
      printf '%s\n' "$kandidat"
      return 0
    fi
  done
  if command -v java >/dev/null 2>&1; then
    v="$(versi_java "$(command -v java)" || true)"
    if [ -n "$v" ] && [ "$v" -ge 17 ] 2>/dev/null; then
      command -v java
      return 0
    fi
  fi
  return 1
}

JAVA_BIN="$(cari_java || true)"
if [ -z "$JAVA_BIN" ]; then
  cat >&2 <<'EOF'
Java 17 belum ditemukan.

Pasang salah satu cara berikut, lalu jalankan lagi skrip ini:

  # Debian / Ubuntu / Kali (butuh sudo)
  sudo apt update && sudo apt install -y openjdk-17-jdk

  # Tanpa sudo — unduh JRE 17 ke ~/.local/jre17 (~45 MB)
  mkdir -p ~/.local/jre17
  curl -L "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jre/hotspot/normal/eclipse" \
    | tar -xz -C ~/.local/jre17 --strip-components=1
EOF
  exit 1
fi
info "Java   : $JAVA_BIN ($("$JAVA_BIN" -version 2>&1 | head -1))"

# --- 2. Pastikan JAR aplikasi tersedia (build bila belum ada) ----------------
if [ ! -f "$APP_JAR" ]; then
  JDK_DIR="$(dirname "$(dirname "$JAVA_BIN")")"
  if [ ! -x "$JDK_DIR/bin/javac" ]; then
    die "File $APP_JAR belum ada dan Java yang ditemukan hanya JRE (tanpa javac).
Pasang JDK 17 (sudo apt install openjdk-17-jdk), lalu jalankan ulang skrip ini."
  fi
  [ -x ./mvnw ] || die "mvnw tidak ditemukan. Jalankan skrip ini dari folder project."
  info "JAR belum ada — membangun aplikasi dengan Maven (sekali saja, butuh internet)..."
  JAVA_HOME="$JDK_DIR" ./mvnw -q -B -DskipTests package
fi

# --- 3. Tentukan mode database ----------------------------------------------
JAVA_OPTS=(-cp "$APP_JAR")
APP_ARGS=()

if [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  # Mode lokal: H2 (driver di luar JAR, dimuat lewat loader.path)
  if [ ! -f "$H2_JAR" ]; then
    info "Mengunduh driver H2 $H2_VERSION ..."
    mkdir -p "$(dirname "$H2_JAR")"
    curl -fL --retry 3 -o "$H2_JAR" "$H2_URL" \
      || die "Gagal mengunduh driver H2 dari $H2_URL"
  fi
  mkdir -p "$DATA_DIR"
  info "Database: H2 file -> $DATA_DIR/$DATA_NAME.mv.db"
  JAVA_OPTS+=(-Dloader.path="$H2_JAR")
  APP_ARGS+=(
    --spring.datasource.url="jdbc:h2:file:$DATA_DIR/$DATA_NAME"
    --spring.datasource.driver-class-name=org.h2.Driver
    --spring.datasource.username=sa
    --spring.datasource.password=
  )
else
  info "Database: eksternal (SPRING_DATASOURCE_URL)"
fi

info "URL    : http://localhost:${PORT:-8080}"
info "Stop   : tekan Ctrl+C"
printf '\n'

exec "$JAVA_BIN" "${JAVA_OPTS[@]}" \
  org.springframework.boot.loader.launch.PropertiesLauncher \
  "${APP_ARGS[@]}" "$@"
