plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.adimarket"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.adimarket"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
//    android {
//        compileSdk = 34  // atau versi terbaru
//
//        defaultConfig {
//            minSdk = 21
//            targetSdk = 34
//        }
//
//        buildToolsVersion = "34.0.0"  // hapus baris ini jika ada, biarkan otomatis
//    }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // PENTING! Untuk GPS Location
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}