plugins {
    id("com.android.application")
    id("org.jmailen.kotlinter") version "5.5.0"
    id("org.owasp.dependencycheck")
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21"
}

android {
    namespace = "com.sudoplatform.sudodiedgeagentexample"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sudoplatform.sudodiedgeagentexample"
        minSdk = 26
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        // The version of Kotlin and Jetpack Compose are tightly linked. See this table.
        // https://developer.android.com/jetpack/androidx/releases/compose-kotlin
        kotlinCompilerExtensionVersion = "1.5.7"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.sudoplatform:sudodiedgeagent:8.0.0")
    // required transitive dep of Edge Agent SDK
    implementation("net.java.dev.jna:jna:5.18.1@aar")

    implementation("com.sudoplatform:sudologging:6.0.0")
    implementation("com.sudoplatform:sudodirelay:5.0.0")
    implementation("com.sudoplatform:sudouser:21.1.0")
    implementation("com.sudoplatform:sudoprofiles:18.0.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.browser:browser:1.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("io.coil-kt:coil-compose:2.7.0")

    val cameraxVersion = "1.6.1"
    implementation("androidx.camera:camera-camera2:${cameraxVersion}")
    implementation("androidx.camera:camera-lifecycle:${cameraxVersion}")
    implementation("androidx.camera:camera-view:${cameraxVersion}")
    implementation("com.google.zxing:core:3.5.4")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


afterEvaluate {
    // NOTE: this must be within `afterEvaluate` to ensure all the configurations have been created before filtering them
    // https://jeremylong.github.io/DependencyCheck/dependency-check-gradle/configuration.html
    dependencyCheck {
        suppressionFile = "${layout.projectDirectory}/../dependency-suppression.xml"
        failBuildOnCVSS = 0.0f
        scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
        nvd {
            // https://github.com/jeremylong/open-vulnerability-cli/tree/main/vulnz#caching-the-nvd-cve-data
            datafeedUrl = "https://anonyome-nist-cve-mirror.s3.amazonaws.com/"
        }
        analyzers {
            assemblyEnabled = false
            centralEnabled = false
            nexus { enabled = false }
            ossIndex {
                username = if (project.hasProperty("ossIndexUsername")) project.property("ossIndexUsername").toString() else ""
                password = if (project.hasProperty("ossIndexPassword")) project.property("ossIndexPassword").toString() else ""
                warnOnlyOnRemoteErrors = true
            }
        }
    }
}

kotlinter {
    reporters = arrayOf("checkstyle", "plain")
}
