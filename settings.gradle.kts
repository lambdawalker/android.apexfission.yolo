pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id in setOf("com.apexfission.android-repo-setup.root", "com.apexfission.android-repo-setup")) {
                useModule("com.github.lambdawalker:gradle.android-repo-setup:${requested.version}")
            }
        }
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "android.apexfission.yolo"

include(":app", ":yolo")
