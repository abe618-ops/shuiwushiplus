plugins { id("com.android.application") }

android {
    namespace = "com.mingyang.webtovideo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mingyang.webtovideo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes { release { isMinifyEnabled = false } }
}

dependencies {
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.activity:activity:1.10.1")
}
