import java.util.Properties

plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Firebase Cloud Messaging needs the google-services plugin, but only works with a
// real `android/app/google-services.json`. Apply it conditionally (same trick as the
// release keystore below) so local/CI builds without the file still compile — push
// just stays inert until the file is dropped in.
if (rootProject.file("app/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// Optional release signing: populated from a gitignored `android/key.properties`
// (see key.properties.example). When it's absent we fall back to debug signing so
// `flutter build` still works for contributors and CI without the secret.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("key.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "com.mawa3id.app"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        // flutter_local_notifications (push) needs core library desugaring.
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.mawa3id.app"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = keystoreProperties["storeFile"]?.let { file(it) }
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }

    buildTypes {
        release {
            // Sign with the real upload key when key.properties is present;
            // otherwise fall back to debug so unsigned/local builds still work.
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

dependencies {
    // Required by flutter_local_notifications for core library desugaring.
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
