# ===== Build stage: kompilasi & paket JAR =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Langsung salin semua lalu paket (lebih cepat untuk build sekali jalan
# seperti Back4App; cache layer tidak banyak membantu di sana)
COPY pom.xml .
COPY src ./src
RUN mvn -q -B -Dmaven.test.skip=true package

# ===== Run stage: JRE ramping =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Folder lampiran surat (ephemeral di hosting gratis — lihat catatan di README)
RUN mkdir -p /app/uploads && chmod 777 /app/uploads
ENV UPLOAD_DIR=/app/uploads

EXPOSE 8080

# Heap & metaspace dibatasi agar aplikasi tetap hidup di container 256 MB
# (Back4App Containers free tier: 0.25 CPU / 256 MB).
# Catatan: ReservedCodeCacheSize jangan terlalu kecil — dengan 16 MB code cache
# sempat penuh dan JIT nonaktif saat import besar. 32 MB cukup untuk C1-only.
# JAVA_OPTS di bawah juga dipakai untuk uji import 1.000 baris (RSS puncak ±235 MB).
# preferIPv4Stack: Neon punya record IPv6; container tanpa rute IPv6 bisa gagal konek.
ENV JAVA_OPTS="-Xms24m -Xmx96m -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=32m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -XX:MaxDirectMemorySize=8m -Djava.net.preferIPv4Stack=true"

# Shell form + exec agar JAVA_OPTS bisa dioverride dari dashboard hosting.
CMD exec java $JAVA_OPTS -jar app.jar
