plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.itasuper.cliente"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "app.itasuper.cliente"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 18
        versionName = "1.0.0"
    }

    // Assinatura release (CI): só configurada quando as variáveis de ambiente
    // KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS e KEY_PASSWORD existem e o
    // arquivo do keystore existe no disco. Sem elas, o build release segue sem
    // assinatura (comportamento atual) e o build local/debug não é afetado.
    // Nenhum valor sensível é hardcoded aqui — tudo vem de variáveis de
    // ambiente (no CI, dos secrets do GitHub Actions).
    signingConfigs {
        val keystorePath = System.getenv("KEYSTORE_FILE")
        val storePasswordValue = System.getenv("KEYSTORE_PASSWORD")
        val keyAliasValue = System.getenv("KEY_ALIAS")
        val keyPasswordValue = System.getenv("KEY_PASSWORD")
        if (keystorePath != null && file(keystorePath).exists()
            && !storePasswordValue.isNullOrBlank()
            && !keyAliasValue.isNullOrBlank()
            && !keyPasswordValue.isNullOrBlank()
        ) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = false
            // Usa a assinatura release quando configurada acima; sem ela,
            // segue sem assinatura (build release não assinado, como hoje).
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
