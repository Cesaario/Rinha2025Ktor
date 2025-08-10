plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.plugin.serialization)
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "app.cesario"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.cio.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.content.negotiation)

    implementation(libs.slf4j.simple)
    implementation(libs.ktor.server.config.yaml)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.redis)
    implementation(libs.coroutines.reactive)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

graalvmNative {
    binaries {
        named("main") {
            fallback.set(false)
            verbose.set(true)

            buildArgs.add("--initialize-at-run-time=kotlin.DeprecationLevel")
            buildArgs.add("--trace-class-initialization=kotlin.DeprecationLevel")
            buildArgs.add("--strict-image-heap")
            buildArgs.add("--enable-monitoring=jfr")
            buildArgs.add("--gc=parallel")
            buildArgs.add("-O2")
            runtimeArgs.add("--gc=parallel")

//            buildArgs.addAll(listOf(
//                "-H:+ReportExceptionStackTraces",
//                "-H:+PrintClassInitialization",
//                "--install-exit-handlers",
//                "--native-image-info",
//                "-R:MaxHeapSize=256m",
//                "-Dkotlinx.coroutines.debug=on",
//                "-Dkotlinx.coroutines.default.parallelism=8",
//                "-H:+InlineBeforeAnalysis",
//                "-H:+ReportUnsupportedElementsAtRuntime",
//                "-H:+ReportExceptionStackTraces",
//                "--no-fallback",
//            ))

            imageName.set("rinhabackend")
        }
    }
}