# Configuração do Supabase — app multiplataforma (Fase 4)

A camada de rede real já está no código (`shared/.../network/SupabaseApi.kt`),
mas os valores de URL e chave **não** estão commitados — o código hoje usa
placeholders e as telas caem para os mocks quando a chamada falha.

## Onde colocar os valores reais

| Plataforma | Arquivo | Como |
|---|---|---|
| Android | `shared/src/androidMain/kotlin/com/example/shared/network/SupabaseConfig.android.kt` | Trocar os placeholders, ou gerar via `BuildConfig` (recomendado: `local.properties` + secrets do CI, como no app cliente atual) |
| iOS | `shared/src/iosMain/kotlin/com/example/shared/network/SupabaseConfig.ios.kt` | Trocar os placeholders, ou ler de `Info.plist`/`xcconfig` (recomendado) |

Valores usados pelo app cliente Android hoje (projeto `qkjhguziuchqsbxzruea`):

- URL: `https://qkjhguziuchqsbxzruea.supabase.co`
- Chave: a `SUPABASE_ANON_KEY` do app cliente (chave **anon**, nunca a `service_role`)

> Nunca commitar a chave real neste repositório público. Use `BuildConfig` /
> `Info.plist` gerados no build ou secrets do GitHub Actions.

## O que já funciona com a rede ligada

- **Home**: lista de lojas reais (`stores_public`), com regra de vitrine
  (fora lojas `pdv_only` e com < 5 produtos visíveis), taxa de entrega,
  horário de funcionamento (fuso America/Sao_Paulo) e taxa VIP via RPC.
- Endpoints prontos mas ainda não ligados às telas: produtos da loja,
  login/cadastro e criação de pedido (telas seguem com mocks + `TODO(fase-rede)`).
