import com.vanniktech.maven.publish.AndroidSingleVariantLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    id("com.vanniktech.maven.publish")
}

val jitpackBuild = providers.gradleProperty("jitpackBuild").orElse("false").map { it.toBoolean() }
val publicationProperties = Properties().apply {
    rootProject.layout.projectDirectory.file("gradle.properties").asFile.inputStream().use { load(it) }
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

    api(libs.androidx.camera.core)

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
    coordinates(
        if (jitpackBuild.get()) "com.github.lambdawalker" else publicationGroup,
        if (jitpackBuild.get()) "android.apexfission.yolo" else publicationArtifact,
        if (jitpackBuild.get()) "yolo~v${project.version}" else project.version.toString(),
    )
    configure(AndroidSingleVariantLibrary(variant = "release", javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    if (!jitpackBuild.get()) publishToMavenCentral()
    // Ordinary builds and the local verification repository never need secrets.
    if (!jitpackBuild.get() && providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()
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
    from(rootProject.file("docs/agents")) { include("**/*.md"); into("docs") }
    from(rootProject.file("LICENSE"))
}
tasks.withType<org.gradle.jvm.tasks.Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

publishing {
    repositories {
        maven {
            name = "verification"
            url = rootProject.layout.buildDirectory.dir("verification-repository").get().asFile.toURI()
        }
    }
}

tasks.configureEach {
    if (name.contains("MavenCentral", ignoreCase = true)) {
        dependsOn(rootProject.tasks.named("verifyPublicationReservation"))
    }
}
