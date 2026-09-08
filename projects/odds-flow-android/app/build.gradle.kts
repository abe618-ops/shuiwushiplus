plugins { id("com.android.application") }

android {
    namespace = "com.abe618.oddsflow"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.abe618.oddsflow"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
}
