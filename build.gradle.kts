// Shared plugin versions; only :yolo configures Maven publishing.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

listOf("generateImportDocs" to "generate", "verifyImportDocs" to "verify").forEach { (taskName, command) ->
    tasks.register<Exec>(taskName) {
        group = "documentation"
        description = "${command.replaceFirstChar { it.uppercase() }} installation documentation from confirmed release metadata"
        workingDir(rootDir)
        commandLine("python3", "scripts/release.py", command)
    }
}
