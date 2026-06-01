plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.linkup.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.linkup.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Add Java Toolchain to ensure a valid JDK is used instead of the broken JRE
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.palette)
    implementation(libs.biometric)
    implementation(libs.gson)
    implementation("com.google.guava:guava:33.0.0-android")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Retrofit & Network (Used for Supabase)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Room components
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // QR Code
    implementation(libs.zxing.core)
    implementation(libs.zxing.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
