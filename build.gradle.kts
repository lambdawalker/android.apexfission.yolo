// AGP's default Kotlin compiler cannot read the released coordinates 2.4 metadata.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

// Shared plugin versions; only :yolo configures Maven publishing.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.apexfission.android-repo-setup.root") version "v0.1.20"
    id("com.apexfission.android-repo-setup") version "v0.1.20" apply false
}
