# ===== Build stage: kompilasi & paket JAR =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Unduh dependensi lebih dulu (cache layer)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

# Kompilasi & paket (lewati test)
COPY src ./src
RUN mvn -q -B -DskipTests package

# ===== Run stage: JRE ramping =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Folder lampiran surat (ephemeral di hosting gratis — lihat catatan di README)
RUN mkdir -p /app/uploads && chmod 777 /app/uploads
ENV UPLOAD_DIR=/app/uploads

EXPOSE 8080
# Render/Koyeb menyuntikkan PORT sendiri; aplikasi membaca ${PORT:8080}
CMD ["java", "-jar", "app.jar"]
