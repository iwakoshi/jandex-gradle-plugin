# Jandex Gradle Plugin

[![CI](https://github.com/iwakoshi/jandex-gradle-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/iwakoshi/jandex-gradle-plugin/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=iwakoshi_jandex-gradle-plugin&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=iwakoshi_jandex-gradle-plugin)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.iwakoshi.jandex)](https://plugins.gradle.org/plugin/io.github.iwakoshi.jandex)
[![License](https://img.shields.io/github/license/iwakoshi/jandex-gradle-plugin)](LICENSE)

A Gradle plugin that generates [Jandex](https://smallrye.io/jandex/) CDI bean indexes (`META-INF/jandex.idx`) for your Java projects. The index is automatically packaged into your JAR, enabling faster bean discovery at runtime for frameworks like Quarkus, CDI, and Hibernate.

## Quick Start

### Groovy DSL (`build.gradle`)

```groovy
plugins {
    id 'java'
    id 'io.github.iwakoshi.jandex' version '0.1.0'
}
```

### Kotlin DSL (`build.gradle.kts`)

```kotlin
plugins {
    java
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

That's it! The plugin automatically:
- Generates the Jandex index after compilation
- Packages `META-INF/jandex.idx` into your JAR
- Supports incremental builds and build cache

## Examples

### Minimal (zero configuration)

```kotlin
plugins {
    java
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

Just apply the plugin. Your JAR will contain `META-INF/jandex.idx` with all your classes indexed.

### Custom Jandex version

```kotlin
plugins {
    java
    id("io.github.iwakoshi.jandex") version "0.1.0"
}

jandex {
    version.set("3.2.0")  // Use a specific Jandex library version
}
```

### Index test classes

```kotlin
plugins {
    java
    id("io.github.iwakoshi.jandex") version "0.1.0"
}

jandex {
    processTestClasses.set(true)  // Also generate index for test source set
}
```

### With java-library plugin

```kotlin
plugins {
    `java-library`
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

Works seamlessly with `java-library`, `application`, and any plugin that applies the `java` plugin.

### Multi-module project

```kotlin
// settings.gradle.kts
rootProject.name = "my-app"
include("core", "web", "api")
```

```kotlin
// core/build.gradle.kts
plugins {
    `java-library`
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

```kotlin
// web/build.gradle.kts
plugins {
    java
    id("io.github.iwakoshi.jandex") version "0.1.0"
}

dependencies {
    implementation(project(":core"))
}
```

Each module generates its own `META-INF/jandex.idx` inside its JAR.

### With Kotlin

```kotlin
plugins {
    kotlin("jvm") version "1.9.25"
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

The plugin indexes all compiled class files, including those generated from Kotlin sources.

### Quarkus project

```kotlin
plugins {
    java
    id("io.quarkus") version "3.8.0"
    id("io.github.iwakoshi.jandex") version "0.1.0"
}
```

Quarkus uses Jandex indexes for bean discovery. With this plugin, your beans are pre-indexed at build time instead of runtime scanning.

### Controlling index format version

```kotlin
jandex {
    indexVersion.set(6)  // Use older index format for compatibility
}
```

Use this if your runtime framework requires an older Jandex index format.

## Configuration Reference

| Property | Type | Default    | Description |
|----------|------|------------|-------------|
| `version` | `String` | `3.6.0`    | The `io.smallrye:jandex` library version |
| `processTestClasses` | `Boolean` | `false`    | Whether to also index test classes |
| `indexVersion` | `Integer` | _(latest)_ | Jandex index format version (>= 6) |

## Tasks

| Task | Description |
|------|-------------|
| `jandex` | Generate Jandex index for the `main` source set |
| `jandexTest` | Generate Jandex index for `test` source set (when `processTestClasses` is enabled) |

The `jandex` task runs automatically as part of `build` and before `jar`.

## Compatibility

| Feature | Status |
|---------|--------|
| Gradle 8.0+ | ✅ |
| Java 8+ | ✅ |
| Configuration cache | ✅ |
| Build cache | ✅ |
| Incremental builds | ✅ |
| `java` plugin | ✅ |
| `java-library` plugin | ✅ |
| `application` plugin | ✅ |
| Kotlin JVM plugin | ✅ |
| Multi-module projects | ✅ |

## Contributing

Contributions are welcome!

## Development

This project uses `asdf` for tool version management and `go-task` (Taskfile) for automation.

### Prerequisites

- [asdf](https://asdf-vm.com/) installed

### Setup

```bash
task setup
```

### Common Commands

| Command | Description |
|---------|-------------|
| `task build` | Build the project |
| `task test` | Run all tests |
| `task clean` | Clean build artifacts |
| `task publish-local` | Publish to local Maven repository |
| `task update-java` | Update to latest Java 8 Corretto |

### Publishing a Release

1. Tag the commit: `git tag v0.1.0`
2. Push the tag: `git push origin v0.1.0`
3. GitHub Actions will automatically publish to Gradle Plugin Portal and GitHub Packages

## License

[Apache License 2.0](LICENSE)
