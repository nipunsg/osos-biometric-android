plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.osos.biometric"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(libs.androidx.biometric)
    implementation(libs.androidx.core.ktx)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.nipunsg"
            artifactId = "osos-biometric-android"
            version = System.getenv("VERSION") ?: "1.0.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
