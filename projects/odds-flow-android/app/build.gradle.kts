plugins { id("com.android.application") }

android {
    namespace = "com.abe618.oddsflow"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.abe618.oddsflow"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    buildTypes { release { isMinifyEnabled = false } }
}
