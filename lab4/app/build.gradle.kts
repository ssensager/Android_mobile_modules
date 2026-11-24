plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.lab4"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.lab4"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity.ktx)
    implementation(libs.recyclerview)
    implementation(libs.glide)

    testImplementation(libs.junit)
}
