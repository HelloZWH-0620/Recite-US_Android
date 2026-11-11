plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.reciteus2.beishuwa"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.reciteus2.beishuwa"
        minSdk = 26
        targetSdk = 36
        versionCode = 6111
        versionName = "1.6.6111"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            // 使用调试签名，保证 release 包可直接安装
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("androidx.media:media:1.7.0")
    implementation("org.jsoup:jsoup:1.18.3")
}
