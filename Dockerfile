# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 4: Native Image Build
FROM ghcr.io/graalvm/graalvm-ce:22.3.2 as native-build
RUN gu install native-image
WORKDIR /home/gradle/app
COPY --from=build /home/gradle/src /home/gradle/app
RUN ./gradlew nativeCompile

# Stage 5: Native Runtime
FROM ghcr.io/graalvm/graalvm-ce:22.3.2 as native-runtime
EXPOSE 9999
WORKDIR /app
COPY --from=native-build /home/gradle/app/build/native/nativeCompile/rinhabackend /app/rinhabackend
ENTRYPOINT ["/app/rinhabackend", "-port=9999"]