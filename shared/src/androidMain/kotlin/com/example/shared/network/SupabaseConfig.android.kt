package com.example.shared.network

import com.example.itasuper.shared.BuildConfig

/**
 * Credenciais do Supabase no Android, lidas do BuildConfig.
 *
 * O BuildConfig é gerado pelo Gradle a partir de:
 * - CI: variáveis de ambiente SUPABASE_URL / SUPABASE_ANON_KEY (GitHub Secrets)
 * - Local: gradle properties supabase.url / supabase.anonKey
 *
 * Se vazio (build local sem configuração), cai nos placeholders e as telas
 * mantêm os mocks (fallback) — ver SUPABASE_SETUP.md.
 */
actual val SUPABASE_URL: String =
    BuildConfig.SUPABASE_URL.ifEmpty { "https://SUA-URL.supabase.co" }

actual val SUPABASE_ANON_KEY: String =
    BuildConfig.SUPABASE_ANON_KEY.ifEmpty { "SUA-ANON-KEY" }
