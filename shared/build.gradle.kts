plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.android.library)
}

// fase-recursos: pacote determinístico para o Res gerado a partir de
// commonMain/composeResources (o default do plugin deriva de group/module,
// que não é estável aqui).
compose {
    resources {
        packageOfResClass = "com.example.shared.generated.resources"
    }
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.compose.material.icons.extended)
            // fase-recursos: drawables/fontes via composeResources (Res.drawable/Res.font).
            implementation(compose.components.resources)
            // Fase 5 item 3: armazenamento chave-valor multiplataforma.
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            // Fase 5 item 4: carregamento de imagens (logo das lojas).
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            // ITEM 5/6: BackHandler do gate legal (LegalBackHandler.android).
            implementation(libs.androidx.activity.compose)
            // Fase 5 item 1: push via Firebase Cloud Messaging (mesmo backend do app nativo).
            implementation(libs.firebase.messaging)
            // Push real: NotificationCompat (exibição da notificação em primeiro plano).
            implementation(libs.androidx.core.ktx)
            // Fase 5 item 2: localização (FusedLocationProviderClient).
            implementation(libs.play.services.location)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.example.itasuper.shared"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        // Credenciais do Supabase via BuildConfig (CI: secrets; local: gradle properties).
        // Nunca commitar valores reais — o fallback vazio mantém os placeholders.
        val supabaseUrl: String = System.getenv("SUPABASE_URL")
            ?: project.findProperty("supabase.url")?.toString() ?: ""
        val supabaseAnonKey: String = System.getenv("SUPABASE_ANON_KEY")
            ?: project.findProperty("supabase.anonKey")?.toString() ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
