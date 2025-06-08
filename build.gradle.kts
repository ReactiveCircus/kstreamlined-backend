import com.netflix.graphql.dgs.codegen.gradle.GenerateJavaTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.getSupportedKotlinVersion
import org.graalvm.buildtools.gradle.tasks.BuildNativeImageTask
import org.graalvm.buildtools.gradle.tasks.GenerateResourcesConfigFile
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.powerAssert)
    alias(libs.plugins.kotlin.noArg)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependencyManagement)
    alias(libs.plugins.dgsCodegen)
    alias(libs.plugins.detekt)
    alias(libs.plugins.graalvmNative)
}

group = "io.github.reactivecircus.kstreamlined.backend"
version = "0.0.1-SNAPSHOT"

noArg {
    annotation("io.github.reactivecircus.kstreamlined.backend.NoArg")
}

dependencyManagement {
    imports {
        mavenBom(libs.dgs.bom.get().toString())
    }
}

graalvmNative {
    metadataRepository {
        enabled.set(true)
    }
    binaries.configureEach {
        javaLauncher = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(24))
            vendor.set(JvmVendorSpec.GRAAL_VM)
        }
        resources.autodetect()
        buildArgs(
            "-R:MaxHeapSize=100m",
        )
    }
}

tasks.withType<BuildNativeImageTask>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM plugin is not compatible with configuration cache.")
}

tasks.withType<GenerateResourcesConfigFile>().configureEach {
    notCompatibleWithConfigurationCache("GraalVM plugin is not compatible with configuration cache.")
}

tasks.bootRun {
    environment(
        envVar("KS_REDIS_REST_URL"),
        envVar("KS_REDIS_REST_TOKEN"),
        envVar("KS_GCLOUD_PROJECT_ID"),
    )
}

fun envVar(name: String): Pair<String, String> {
    return name to (providers.environmentVariable(name).orElse(providers.gradleProperty(name)).orNull
        ?: error("Missing environment variable or Gradle property: $name"))
}

tasks.withType<GenerateJavaTask>().configureEach {
    packageName = "io.github.reactivecircus.kstreamlined.backend.schema.generated"
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(getSupportedKotlinVersion())
        }
    }
}

detekt {
    source.setFrom(files("src/"))
    config.setFrom(files("${project.rootDir}/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = true
}
tasks.withType<Detekt>().configureEach {
    jvmTarget = JvmTarget.JVM_21.target
    reports {
        html.outputLocation.set(file("build/reports/detekt/${project.name}.html"))
    }
}
dependencies.add("detektPlugins", libs.detektFormatting)

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.dgs.starter)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.contentNegotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.serialization.xml)
    implementation(libs.gcloud.firestore)
    implementation(libs.caffeine)
    implementation(libs.scrapeit)

    testImplementation(kotlin("test"))
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.ktor.client.mock)
}
