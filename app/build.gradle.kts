import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// 签名口令放在仅本地保存的 keystore/keystore.properties，仓库中不提交任何密钥信息。
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.reciteus2.beishuwa"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.reciteus2.beishuwa"
        minSdk = 26
        targetSdk = 36
        versionCode = 1006004
        versionName = "1.6.4"
    }

    buildFeatures {
        compose = true
    }

    // 固定使用仓库内的签名密钥，禁止改回自动生成的 debug 签名：
    // 该密钥与 1.6.1122 正式包一致，保证 1.6.1122 及以上版本可覆盖升级；
    // 自动生成的 debug 签名每台机器/每次 CI 都不同，会导致 INSTALL_FAILED_UPDATE_INCOMPATIBLE。
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/beishuwa.jks")
            storePassword = keystoreProperties.getProperty("storePassword").orEmpty()
            keyAlias = keystoreProperties.getProperty("keyAlias") ?: "androiddebugkey"
            keyPassword = keystoreProperties.getProperty("keyPassword").orEmpty()
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui:1.12.1")
    implementation("androidx.compose.ui:ui-graphics:1.12.1")
    implementation("androidx.compose.foundation:foundation:1.12.1")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("io.github.feice11:mochi-compose:0.3.0")
    implementation("io.github.feice11:mochi-ios:0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
    implementation("androidx.media:media:1.7.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
