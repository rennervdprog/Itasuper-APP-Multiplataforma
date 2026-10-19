# Configuração do Supabase — app multiplataforma

## Status: credenciais configuradas via CI ✅

As credenciais estão configuradas como **GitHub Secrets** no repositório:
- `SUPABASE_URL` = `https://qkjhguziuchqsbxzruea.supabase.co`
- `SUPABASE_ANON_KEY` = (chave anon do projeto)

O workflow `.github/workflows/build.yml` injeta esses secrets como variáveis de
ambiente no build Android. O `shared/build.gradle.kts` gera o `BuildConfig` com
`buildConfigField` a partir das env vars (com fallback para gradle properties
`supabase.url` / `supabase.anonKey` em builds locais).

`SupabaseConfig.android.kt` lê do `BuildConfig`, com fallback para placeholders
se vazio — nesse caso as telas mantêm os mocks.

> A chave real NUNCA aparece no código. Ela vive apenas nos GitHub Secrets.

## Build local (opcional)

Para testar com a rede real no seu PC, adicione ao `~/.gradle/gradle.properties`:
```properties
supabase.url=https://qkjhguziuchqsbxzruea.supabase.co
supabase.anonKey=SUA-CHAVE-AQUI
```

## iOS

O `SupabaseConfig.ios.kt` ainda usa placeholders. Para ligar a rede no iOS:
1. Adicionar as credenciais como secrets/variáveis no job `build-ios`
2. Gerar um arquivo `SupabaseSecrets.xcconfig` ou injetar via `Info.plist`
3. Ler via `NSBundle.mainBundle` no `SupabaseConfig.ios.kt`

## O que está ligado na rede real

- **Home**: lista de lojas reais (`fetchActiveStores`)
- **Vitrine da loja**: produtos reais (`fetchProductsForStore`), com fallback pros mocks
- **Login**: tenta `signIn` real, com fallback pro mock se falhar
- **Bordas pizza/pastel, adicionais**: já usavam a API

## Pendente

- **Cadastro** (`signUp`): API pronta, falta ligar na tela
- **Criar pedido** (`submitOrder`): API pronta, mas precisa de persistência de sessão
  (clientId + accessToken) — ainda não implementada
- **iOS**: credenciais ainda são placeholders
