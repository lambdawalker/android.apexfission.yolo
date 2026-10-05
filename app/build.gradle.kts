import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Immutable source and checksum: the demo model never enters the library AAR.
val modelUrl = "https://raw.githubusercontent.com/lambdawalker/android.card_detection_lite/115571941f890c4de0324cd008cff31a2068e722/tfmodel/src/main/assets/cdl/tflite/Y11-640E197F16.tflite"
val modelSha256 = "473f497ca45b4ab2f66ef59b3da1c0e27a0ac11227279ba6d89a19566c450e1e"
val demoAssets = layout.buildDirectory.dir("generated/demoAssets")
val prepareDemoModel = tasks.register("prepareDemoModel") {
    inputs.property("url", modelUrl)
    inputs.property("sha256", modelSha256)
    outputs.file(demoAssets.map { it.file("demo.tflite") })
    doLast {
        val model = demoAssets.get().file("demo.tflite").asFile
        model.parentFile.mkdirs()
        val temporary = model.resolveSibling("demo.tflite.part")
        try {
            val connection = URI(modelUrl).toURL().openConnection().apply {
                connectTimeout = 30_000
                readTimeout = 60_000
            }
            connection.getInputStream().use { input -> temporary.outputStream().use { input.copyTo(it) } }
            val digest = MessageDigest.getInstance("SHA-256").digest(temporary.readBytes())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            check(digest == modelSha256) { "Demo model checksum mismatch" }
            temporary.copyTo(model, overwrite = true)
        } finally {
            temporary.delete()
        }
    }
}

android {
    namespace = "com.apexfission.android.yolo.demo"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.apexfission.android.yolo.demo"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    androidResources { noCompress += "tflite" }
    sourceSets.getByName("main").assets.srcDir(demoAssets)
}
kotlin { jvmToolchain(17) }
tasks.named("preBuild") { dependsOn(prepareDemoModel) }

dependencies {
    implementation(project(":yolo"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
