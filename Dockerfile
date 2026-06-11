# syntax=docker/dockerfile:1

# ---------- Stage 1: build the Next.js frontend (static export) ----------
FROM node:22-alpine AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build
# static site lands in /app/out

# ---------- Stage 2: build the Spring Boot backend (Java 25) ----------
FROM maven:3.9-eclipse-temurin-25 AS backend
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
# bundle the exported frontend into the jar's static resources
COPY --from=frontend /app/out ./src/main/resources/static
RUN mvn -q package

# ---------- Stage 3: minimal runtime ----------
FROM eclipse-temurin:25-jre
RUN useradd --system --uid 1001 appuser
WORKDIR /app
COPY --from=backend /app/target/fractal-puzzle-*.jar app.jar
USER appuser
EXPOSE 8080
ENV JAVA_OPTS=""
HEALTHCHECK --interval=30s --timeout=4s --start-period=30s --retries=3 \
  CMD ["bash", "-c", "exec 3<>/dev/tcp/localhost/8080 && printf 'GET /actuator/health HTTP/1.0\\r\\n\\r\\n' >&3 && grep -q UP <&3"]
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
