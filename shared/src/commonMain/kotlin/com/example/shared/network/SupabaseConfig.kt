package com.example.shared.network

/**
 * Fase 4 (rede real): URL e chave anônima do Supabase, por plataforma.
 *
 * Os valores REAIS nunca ficam no código. Cada plataforma fornece os seus
 * em androidMain/iosMain — hoje com placeholders; o Renner precisa configurar
 * os valores de produção (ver SUPABASE_SETUP.md na raiz do repo).
 * Sem valores reais, as chamadas falham e as telas mostram erro/vazio —
 * nunca dados de exemplo (regra do projeto).
 */
expect val SUPABASE_URL: String

expect val SUPABASE_ANON_KEY: String
