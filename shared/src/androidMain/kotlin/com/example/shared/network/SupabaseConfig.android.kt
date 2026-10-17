package com.example.shared.network

// TODO(fase-rede): substituir pelos valores reais de produção.
// Caminho recomendado: BuildConfig gerado de local.properties / secrets do CI
// (o app cliente Android usa SUPABASE_URL/SUPABASE_ANON_KEY vindos do BuildConfig).
// Ver SUPABASE_SETUP.md na raiz do repo.
actual val SUPABASE_URL: String = "https://SUA-URL.supabase.co"

actual val SUPABASE_ANON_KEY: String = "SUA-ANON-KEY"
