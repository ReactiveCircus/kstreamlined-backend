# Build stage
FROM ghcr.io/graalvm/graalvm-community:22 AS build

WORKDIR /app

COPY . .

RUN ./gradlew nativeCompile --no-configuration-cache

# Runtime stage
FROM ubuntu:latest

COPY --from=build /app/build/native/nativeCompile/kstreamlined-backend /app/kstreamlined-backend

ENTRYPOINT ["/app/kstreamlined-backend"]
