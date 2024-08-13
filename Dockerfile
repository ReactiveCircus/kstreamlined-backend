FROM scratch

COPY build/native/nativeCompile/kstreamlined-backend /app/kstreamlined-backend

ENTRYPOINT ["/app/kstreamlined-backend"]
