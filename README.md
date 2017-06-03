# aether-gradle-plugin

A Gradle plugin which uses [Aether](https://projects.eclipse.org/projects/technology.aether) to resolve transitive dependencies instead of Gradle

Resolves [this issue](https://github.com/gradle/gradle/issues/2212)

Build script snippet for use in all Gradle versions:

```
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.co.kaiba.gradle:aether-gradle-plugin:1.0"
  }
}

apply plugin: "co.kaiba.gradle.aether"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:

```
plugins {
  id "co.kaiba.gradle.aether" version "1.0"
}
```