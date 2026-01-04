plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.kitchen_manager"
    compileSdk = 35

    buildFeatures {
        viewBinding=true
    }

    defaultConfig {
        applicationId = "com.example.kitchen_manager"
        minSdk = 24
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
}

dependencies {
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    // Retrofit 网络请求库
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")

    // Gson 用于 JSON 解析
    implementation ("com.google.code.gson:gson:2.8.9")
    // ML Kit 图像标注
    implementation ("com.google.mlkit:image-labeling:17.0.5")

    // CameraX 核心库
    implementation ("androidx.camera:camera-core:1.4.0")
    implementation ("androidx.camera:camera-lifecycle:1.4.0")
    implementation ("androidx.camera:camera-view:1.4.0")
    // JSON处理
    implementation ("org.json:json:20231013")
    // 权限处理
    implementation ("androidx.activity:activity-ktx:1.9.0")
    implementation ("androidx.fragment:fragment-ktx:1.7.0")

    implementation("com.google.android.material:material:1.11.0")
    // RecyclerView
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.volley)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}