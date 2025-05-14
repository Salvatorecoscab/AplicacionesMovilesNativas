import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.kapt")

}

android {
    namespace = "com.example.locationmaps"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.locationmaps"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        //load the values from .properties file
        val keystoreFile = project.rootProject.file("local.properties")
        val properties = Properties()
        properties.load(keystoreFile.inputStream())

        //return empty key in case something goes wrong
        val apiKey = properties.getProperty("API_KEY") ?: ""
        buildConfigField("String", "API_KEY", apiKey)

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
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)

    // Room Database
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation(libs.play.services.maps)
    kapt("androidx.room:room-compiler:2.7.1")  // Usa kapt en lugar de annotationProcessor

    // Retrofit para APIs de POIs
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Coil para carga de imágenes
    implementation(libs.coil)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // Usa la versión más reciente
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Usa la versión más reciente
    // ViewModel y LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.photoview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}