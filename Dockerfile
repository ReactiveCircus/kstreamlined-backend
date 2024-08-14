# Build stage
FROM ghcr.io/graalvm/graalvm-community:22 as build

COPY . .

RUN ./gradlew nativeCompile --no-configuration-cache

# Runtime stage
FROM docker.io/oraclelinux:8-slim

COPY --from=build /build/native/nativeCompile/kstreamlined-backend /app/kstreamlined-backend

ENTRYPOINT ["/app/kstreamlined-backend"]
