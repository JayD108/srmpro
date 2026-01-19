plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.srmistpro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.srmist_pro"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // Graphs & JSON
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
}