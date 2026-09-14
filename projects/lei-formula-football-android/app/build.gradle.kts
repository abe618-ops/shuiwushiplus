plugins {
    id("com.android.application")
}

android {
    namespace = "com.shuiwushiplus.leiformula"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shuiwushiplus.leiformula"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

plugins.apply("org.jetbrains.kotlin.android")

dependencies {
    implementation("cn.6tail:lunar:1.7.7")
}
