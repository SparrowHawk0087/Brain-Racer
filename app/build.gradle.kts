import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) load(f.inputStream())
}

android {
    namespace = "com.example.brainracer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.brainracer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cloud.ru Evolution Object Storage: s3.access.key = "{tenantId}:{keyId}" (см. документацию API-ключа),
        // s3.secret.key = Key Secret. Только Key ID без tenant → ошибка AWS "Access Key Id you provided...".
        buildConfigField(
            "String", "S3_ACCESS_KEY",
            "\"${localProperties.getProperty("s3.access.key", "")}\""
        )
        buildConfigField(
            "String", "S3_SECRET_KEY",
            "\"${localProperties.getProperty("s3.secret.key", "")}\""
        )
        // Базовый URL для анонимного GET аватаров. Cloud.ru: формат зависит от того,
        // что задано в консоли — Глобальное название → https://global.s3.cloud.ru/<имя>;
        // Доменное имя → https://<имя>.s3.cloud.ru. См. StorageConfig.
        buildConfigField(
            "String", "S3_AVATAR_PUBLIC_BASE_URL",
            "\"${localProperties.getProperty(
                "s3.avatar.public.base",
                "https://global.s3.cloud.ru/brainracer-avatars-public"
            )}\""
        )
        // Базовый URL для анонимного GET обложек и картинок вопросов викторин.
        buildConfigField(
            "String", "S3_QUIZ_PUBLIC_BASE_URL",
            "\"${localProperties.getProperty(
                "s3.quiz.public.base",
                "https://global.s3.cloud.ru/brainracer-quizzes-public"
            )}\""
        )
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
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.foundation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation("com.amazonaws:aws-android-sdk-s3:2.75.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation(libs.google.play.services.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation("dnsjava:dnsjava:3.5.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(libs.androidx.core.splashscreen)
}