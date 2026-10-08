<!-- GENERATED FILE. Source: docs/templates/IMPORT.md.template. Run ./gradlew generateImportDocs. -->
# Install Apexfission YOLO

This is the authoritative latest confirmed installation reference. Choose one destination and one dependency syntax. Pending uploads and tags are not proof of availability.

## yolo: yolo

Confirmed version: **0.1.0**. Source: [76abd72e4e00b2b0ed316e333aff35f6fa989a91](https://github.com/lambdawalker/android.apexfission.yolo/commit/76abd72e4e00b2b0ed316e333aff35f6fa989a91).

Choose **one** destination below and **one** dependency syntax. Each destination provides this same release; do not add duplicate dependencies.

### maven-central

Repository: **maven-central**.

#### Gradle Kotlin DSL

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

In the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.apexfission.android:yolo:0.1.0")
}
```

#### Gradle Groovy DSL

In `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

In the app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.apexfission.android:yolo:0.1.0'
}
```

#### Version catalog

Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:

```toml
[libraries]
yolo = { module = "com.apexfission.android:yolo", version = "0.1.0" }
```

Then use this instead of the direct dependency in the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.yolo)
}
```

#### Maven

Add these repositories and dependency to `pom.xml`:

```xml
<repositories>
  <repository>
    <id>google</id>
    <url>https://dl.google.com/dl/android/maven2</url>
  </repository>
  <repository>
    <id>central</id>
    <url>https://repo.maven.apache.org/maven2</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.apexfission.android</groupId>
    <artifactId>yolo</artifactId>
    <version>0.1.0</version>
    <type>aar</type>
  </dependency>
</dependencies>
```

## Requirements

Android minSdk 28. The library targets JVM 17; the checked-in Gradle daemon uses Java 21 and compileSdk 37. Geometry types are exported through the explicit public Coordinates dependency. The demo uses the local project dependency.

See [quickstart](docs/agents/quickstart.md), [release guide](docs/releases.md), and the website's exact-version archive for matching historical guides. Never guess a released version or coordinates.
