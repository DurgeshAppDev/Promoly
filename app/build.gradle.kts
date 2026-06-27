plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.durgesh.promoly"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.durgesh.promoly"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Enables code shrinking, obfuscation, and optimization
            isMinifyEnabled = true

            // Enables resource shrinking (removes unused images/layouts)
            isShrinkResources = true

            // Tells R8 which rules files to use
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
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/INDEX.LIST"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)

    // Modern Google Sign-In (Credential Manager)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Firebase (Using Bill of Materials BOM for uniform versions)
    implementation(platform("com.google.firebase:firebase-bom:34.15.0"))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation("com.google.firebase:firebase-storage")

    // Fix for Firestore gRPC crash
    implementation("io.grpc:grpc-android:1.82.1")
    implementation("io.grpc:grpc-okhttp:1.82.1")
    implementation("io.grpc:grpc-stub:1.82.1")
    implementation("io.grpc:grpc-protobuf-lite:1.82.1")

    // Glide Image Loader
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // OkHttp for FCM Notifications
    implementation(libs.okhttp)
    
    // Google Auth for FCM V1
    implementation("com.google.auth:google-auth-library-oauth2-http:1.48.0")

    // Responsive UI Dimensions
    implementation("com.intuit.sdp:sdp-android:1.1.1")
    implementation("com.intuit.ssp:ssp-android:1.1.1")

    // Image Cropper
    implementation("com.github.CanHub:Android-Image-Cropper:4.5.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}