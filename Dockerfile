FROM scratch

COPY build/native/nativeCompile/kstreamlined-backend /app/kstreamlined-backend

RUN chmod +x /app/kstreamlined-backend

ENTRYPOINT ["/app/kstreamlined-backend"]
