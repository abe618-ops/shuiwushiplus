plugins {
    id("com.android.application")
}

android {
    namespace = "com.abe618.liurenfootball"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.abe618.liurenfootball.v1"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
