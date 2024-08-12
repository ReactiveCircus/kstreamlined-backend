# Build stage
FROM ghcr.io/graalvm/graalvm-community:22 as build

WORKDIR /app

COPY . /app

RUN ./gradlew nativeCompile --no-configuration-cache

# Runtime stage
FROM scratch

COPY --from=build /app/build/native/nativeCompile/kstreamlined-backend /app

ENTRYPOINT ["/app/"]

EXPOSE 8000
