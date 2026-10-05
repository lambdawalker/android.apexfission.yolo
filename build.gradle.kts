import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish") version "0.37.0"
}

val releaseVersion = providers.gradleProperty("releaseVersion")
require(releaseVersion.orNull?.matches(Regex("(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)")) != false) {
    "releaseVersion must be a stable X.Y.Z version"
}
val publicationProperties = Properties().apply {
    layout.projectDirectory.file("gradle.properties").asFile.inputStream().use { load(it) }
}
val publicationGroup = publicationProperties.getProperty("GROUP")
val publicationArtifact = publicationProperties.getProperty("POM_ARTIFACT_ID")
val projectUrl = "https://github.com/lambdawalker/android.apexfission.yolo"


android {
    namespace = "com.apexfission.android.yolo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Geometry types appear in public Detection/Feature signatures.
    api(publicationProperties.getProperty("COORDINATES_DEPENDENCY"))

    implementation(libs.androidx.camera.core)

    implementation(libs.litert.gpu)
    implementation(libs.litert.support) {
        exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

mavenPublishing {
    // Set coordinates before creating publications, which finalizes these values.
    coordinates(publicationGroup, publicationArtifact, releaseVersion.orElse("0.0.0-SNAPSHOT").get())
    configure(AndroidSingleVariantLibrary(variant = "release", javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()
    // Ordinary builds and the local verification repository never need secrets.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
    pom {
        name.set("Apexfission YOLO")
        description.set("Android YOLO detection with LiteRT inference, image preprocessing, and NMS post-processing.")
        inceptionYear.set("2026")
        url.set(projectUrl)
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("lambdawalker")
                name.set("David Garcia")
                url.set("https://github.com/lambdawalker")
            }
        }
        scm {
            url.set(projectUrl)
            connection.set("scm:git:$projectUrl.git")
            developerConnection.set("scm:git:ssh://git@github.com/lambdawalker/android.apexfission.yolo.git")
        }
    }
}

// Ship the maintained guides in the documentation classifier; do not pretend
// that Java's javadoc tool generates an API reference for Kotlin sources.
tasks.withType<com.vanniktech.maven.publish.tasks.JavadocJar>().configureEach {
    from("README.md", "LICENSE")
    from("src/main/java") { include("**/README.md"); into("packages") }
}
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    repositories {
        maven {
            name = "verification"
            url = layout.buildDirectory.dir("verification-repository").get().asFile.toURI()
        }
    }
}

val verifyCentralReservation = tasks.register<Exec>("verifyCentralReservation") {
    group = "publishing"
    workingDir(projectDir)
    commandLine("python3", "scripts/release.py", "guard", "--version", releaseVersion.orElse("").get())
    doFirst {
        listOf("mavenCentralUsername", "mavenCentralPassword", "signingInMemoryKey").forEach {
            require(!providers.gradleProperty(it).orNull.isNullOrBlank()) { "Missing release credential: $it" }
        }
    }
}
// Guard upload and aggregate/close/release tasks, not just the final lifecycle task.
tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true)) dependsOn(verifyCentralReservation)
}

listOf("generateImportDocs" to "generate", "verifyImportDocs" to "verify").forEach { (taskName, command) ->
    tasks.register<Exec>(taskName) {
        group = "documentation"
        description = "${command.replaceFirstChar { it.uppercase() }} installation documentation from confirmed release metadata"
        workingDir(projectDir)
        commandLine("python3", "scripts/release.py", command)
    }
}
