plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")

}

android {
    namespace = "com.example.sentryone"
    compileSdk = 35
    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.sentryone"
        minSdk = 28
        targetSdk = 35
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

// Room (using KSP)
    val roomVersion = "2.6.1" // Or use "2.7.0-alpha01" if you really need it and understand alpha stability
    // For stable, 2.6.1 is the latest as of my last update.
    // Check official docs for the absolute latest stable.
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion") // For Coroutines and Flow support

    ksp("androidx.room:room-compiler:$roomVersion")
    // REMOVE: annotationProcessor("androidx.room:room-compiler:$room_version")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Use a recent stable version

    // ViewModel & LiveData (Lifecycle)
    val lifecycleVersion = "2.6.2" // Or a more recent stable version
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    // lifecycle-runtime-ktx is often included transitively by other lifecycle artifacts,
    // but good to be explicit if you use LifecycleScope directly often.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycleVersion")


    // Fragment KTX (includes support for by viewModels(), etc.)
    implementation("androidx.fragment:fragment-ktx:1.6.2") // Use a recent stable version

//    jetpack datastore dependency to store the app setting
    implementation("androidx.datastore:datastore-preferences:1.1.7")

//    material component dependency
    implementation("com.google.android.material:material:1.14.0-alpha01")
}