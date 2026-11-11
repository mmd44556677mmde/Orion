plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.mohammad.orion"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.mohammad.orion"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-debug" }
        release { isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    lint { abortOnError = true; checkReleaseBuilds = true }
    testOptions { unitTests.isReturnDefaultValues = true }
    packaging { resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}") }
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.splash)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso)
}
