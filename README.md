# ItaSuper — App Multiplataforma (Android + iOS)

App cliente ItaSuper em **Compose Multiplatform**: um único código Kotlin que gera apps nativos para Android e iPhone.

> **Fase 1:** estrutura base com builds funcionando. As telas do app atual (`Itasuper-APP-NATIVO`) serão migradas nas próximas fases. O repositório original **não foi tocado**.

## Estrutura

```
├── shared/                  # Código compartilhado (Kotlin)
│   └── src/
│       ├── commonMain/      # Telas, lógica, modelos — roda nos dois
│       ├── androidMain/     # Só Android (ex: platformName = "Android")
│       └── iosMain/         # Só iOS (ex: MainViewController p/ SwiftUI)
├── androidApp/              # App Android (minSdk 24, v1.0.0)
├── iosApp/                  # App iOS (projeto Xcode + SwiftUI)
├── gradle/libs.versions.toml # Versões centralizadas
└── .github/workflows/build.yml
```

## Versões

| Componente | Versão |
|---|---|
| Kotlin | 2.1.20 |
| Compose Multiplatform | 1.7.3 |
| Android Gradle Plugin | 8.7.3 |
| compileSdk / targetSdk | 35 |
| minSdk (Android) | 24 |

## Builds (GitHub Actions)

Todo push na `main` roda dois jobs:

| Job | Runner | Resultado |
|---|---|---|
| `build-android` | `ubuntu-latest` | APK debug (`android-debug-apk`) |
| `build-ios` | `macos-latest` | Framework iOS compilado (sem assinatura) |

O build iOS compila o framework Kotlin/Native para validar que o código compartilhado funciona no iPhone. **Assinatura e distribuição** (TestFlight/App Store) exigem a conta Apple Developer — Fase 2.

## Desenvolvimento local

```bash
# Android
./gradlew :androidApp:assembleDebug

# Framework iOS (precisa de Mac com Xcode)
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

## Fases

- [x] **Fase 1** — estrutura base, builds verde no CI
- [ ] **Fase 2** — migrar camada de rede (OkHttp → Ktor) e telas
- [ ] **Fase 3** — assinatura iOS (conta Apple Developer do Renner)
