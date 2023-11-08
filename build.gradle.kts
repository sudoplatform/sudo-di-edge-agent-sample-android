// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        mavenLocal()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
        classpath("org.owasp:dependency-check-gradle:8.4.0")
    }
}

allprojects {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}