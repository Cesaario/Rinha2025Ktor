FROM ghcr.io/graalvm/graalvm-ce:22.3.1 as native-build
RUN gu install native-image
WORKDIR /app
COPY . .
RUN ./gradlew buildFatJar
RUN native-image --no-fallback --static -jar build/libs/backendrinha.jar app

FROM scratch
COPY --from=native-build /app/app /app
ENTRYPOINT ["/app"]
