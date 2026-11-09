# 19 — Tema claro e escuro persistente — Design

## Visão geral

```
core/preferences/domain/ThemeMode.kt                    (novo: SYSTEM, LIGHT, DARK + storageId estável)
core/preferences/domain/ThemePreferenceRepository.kt    (novo: observe(): Flow<ThemeMode>, suspend set(mode))
core/preferences/data/ThemePreferenceRepositoryImpl.kt  (novo: mesmo DataStore, chave "theme_mode")
core/preferences/di/PreferencesModule.kt                (alterado: registra o repositório)
core/designsystem/Color.kt                              (alterado: paleta clara)
core/designsystem/Theme.kt                              (alterado: AndroidEventSchedulerTheme(darkTheme = true, …))
core/designsystem/ThemeResolution.kt                    (novo: ThemeMode.isDark(systemDark) — regra pura)
core/ui/theme/SystemThemeEffects.kt                     (novo: barras do sistema + fundo da janela pelo tema)
feature/settings/…                                      (alterado: seção Aparência)
MainActivity.kt                                         (alterado: lê o tema, segura a Splash, aplica)
feature/alarm/presentation/screen/FullScreenAlarmActivity.kt (alterado: aplica o mesmo tema)
```
Nenhuma permissão, rota ou dependência nova. Nenhum UseCase.

## Decisões

| Decisão | Escolha | Alternativa rejeitada e motivo |
|---|---|---|
| Armazenamento | **Preferences DataStore** existente (`remember_me_prefs`), chave `theme_mode` | `SharedPreferences`: segundo mecanismo de persistência, API síncrona na main thread (a única vantagem real dele aqui, a leitura síncrona na partida, é resolvida com a Splash). Proto DataStore e Room: esquema e dependência para um único valor. Detalhes em [`docs/decisoes/persistencia-do-tema-datastore-vs-sharedpreferences.md`](../../../docs/decisoes/persistencia-do-tema-datastore-vs-sharedpreferences.md) |
| Repositório | `ThemePreferenceRepository` **próprio**, no mesmo arquivo de DataStore | Campo novo em `UserSettings`: o `UserSettingsRepository.save()` regrava **todas** as chaves e Configurações só grava no botão Salvar, o que exigiria salvar o formulário inteiro ao trocar o tema (violaria R1.3). O padrão de repositório por assunto no mesmo DataStore já existe (`HomePreferencesRepository`, spec 06) |
| Onde fica o `ThemeMode` | `core.preferences.domain` | `feature.settings`: a `FullScreenAlarmActivity` (`feature.alarm`) e a `MainActivity` também o leem, e uma feature não importa outra |
| Valor padrão | **Escuro** | Seguir o sistema: mais comum em apps Android, mas mudaria o visual de quem já usa o app com o sistema claro, sem ninguém ter pedido. Pode ser revisto como decisão de produto (troca de uma constante) |
| Partida sem piscar | `installSplashScreen().setKeepOnScreenCondition { themeMode.value == null }` | Ler síncrono (`runBlocking { first() }`) no `onCreate`: I/O na main thread. `SharedPreferences` síncrono: ver acima |
| Fundo da SplashScreen | **Escuro da marca nos dois temas** | `values-night/`: o qualificador `-night` segue o **sistema**, não a preferência do app. Com sistema escuro e app claro, a Splash ficaria escura de qualquer jeito, e pior: de forma inconsistente entre aparelhos. Splash fixa é previsível |
| Troca sem recriar | O tema é estado Compose (`darkTheme` vem de um `StateFlow`) | `recreate()` / `AppCompatDelegate.setDefaultNightMode`: o projeto não usa AppCompat (`android:Theme.Material`), e recriar perde o estado transitório da tela |

### Decisões de UseCase
Nenhum: ler e gravar um valor é passa-direto de repositório; a resolução claro/escuro é uma função pura de apresentação.

## Contratos

```kotlin
// core/preferences/domain
enum class ThemeMode(val storageId: String) {
    SYSTEM("system"), LIGHT("light"), DARK("dark");
    companion object {
        val DEFAULT = DARK
        fun fromStorageId(id: String?): ThemeMode = entries.firstOrNull { it.storageId == id } ?: DEFAULT
    }
}

interface ThemePreferenceRepository {
    /** Emite o padrão quando nada foi gravado, o valor é desconhecido ou a leitura falha (R2.3). */
    fun observe(): Flow<ThemeMode>
    suspend fun set(mode: ThemeMode)
}

// core/designsystem/ThemeResolution.kt (puro, sem Compose)
fun ThemeMode.isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDarkTheme
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
```
`core.designsystem` passa a depender de `core.preferences.domain` (só do enum). Se isso incomodar a direção das dependências, o enum pode ir para `core.designsystem` e o repositório o importa. Registrar a escolha no PR.

`ThemePreferenceRepositoryImpl`: `dataStore.data.catch { IOException → emit(emptyPreferences()) }.map { ThemeMode.fromStorageId(it[THEME_MODE]) }.distinctUntilChanged()`; `set` → `dataStore.edit { it[THEME_MODE] = mode.storageId }`. Mesmo padrão do `UserSettingsRepositoryImpl`.

### Tema
```kotlin
@Composable
fun AndroidEventSchedulerTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) RememberMeDarkColorScheme else RememberMeLightColorScheme, typography = Typography, content = content)
}
```
O padrão `true` mantém todas as previews e testes existentes no escuro (R5.2). As raízes reais (`MainActivity`, `FullScreenAlarmActivity`) passam o valor resolvido.

Paleta clara proposta (contraste calculado com a fórmula WCAG 2.x):

| Papel | Claro | Par conferido | Contraste |
|---|---|---|---|
| `background` | `#FAFAFA` | `onBackground #1A1A1A` | 16,67:1 |
| `surface` / `surfaceContainer` | `#FFFFFF` | `onSurface #1A1A1A` | 17,40:1 |
| `surfaceVariant` / `surfaceContainerHigh` | `#F2F2F7` | `onSurfaceVariant #545458` | 6,75:1 (7,54:1 sobre `surface`) |
| `primary` / `secondary` | `#00727F` | sobre `background` | 5,42:1 |
| `onPrimary` / `onSecondary` | `#FFFFFF` | sobre `primary` | 5,66:1 |
| `error` | `#C62828` | sobre `surface` | 5,62:1 |
| `outline` | `#C7C7CC` | decorativo | — |

O escuro atual também passa: o pior par é `error`/`surface`, com 5,14:1.

### Raiz da `MainActivity`
```kotlin
private val themeMode: StateFlow<ThemeMode?> by lazy {
    get<ThemePreferenceRepository>().observe().stateIn(lifecycleScope, SharingStarted.Eagerly, null)
}

override fun onCreate(…) {
    installSplashScreen().setKeepOnScreenCondition { themeMode.value == null }   // R3.4
    super.onCreate(…)
    setContent {
        val mode by themeMode.collectAsStateWithLifecycle()
        val resolved = mode ?: return@setContent            // a Splash ainda cobre a tela
        val dark = resolved.isDark(isSystemInDarkTheme())
        AndroidEventSchedulerTheme(darkTheme = dark) {
            SystemThemeEffects(darkTheme = dark)            // barras + fundo da janela (R3.3)
            AppNavHost()
        }
    }
}
```
A leitura do DataStore leva poucos milissegundos depois da primeira (o DataStore guarda em memória), então a Splash não fica mais tempo visível de forma perceptível. Na primeira leitura do processo, o custo é o mesmo que a Splash já cobria.

`SystemThemeEffects` (`core/ui/theme`): num `DisposableEffect(darkTheme)`, chama `enableEdgeToEdge(statusBarStyle = if (dark) SystemBarStyle.dark(TRANSPARENT) else SystemBarStyle.light(TRANSPARENT, TRANSPARENT), navigationBarStyle = …)` na `ComponentActivity` atual e `window.setBackgroundDrawable(ColorDrawable(colorScheme.background.toArgb()))`. O `enableEdgeToEdge` pode ser chamado de novo para trocar o estilo.

### `FullScreenAlarmActivity`
Lê o mesmo repositório (`koinInject` ou `get()`). Enquanto o valor é `null`, não compõe o conteúdo: a janela continua com o fundo escuro do tema XML por poucos milissegundos, sem Splash. Depois aplica `AndroidEventSchedulerTheme(darkTheme = …)` e `SystemThemeEffects`.

### Configurações
- `SettingsUiState.Content` ganha `themeMode: ThemeMode`, alimentado por `combine` com `ThemePreferenceRepository.observe()`. Fica **fora** do `draft`, porque não passa pelo Salvar.
- Família nova `SettingsUiEvent.AppearanceEvent` com `OnThemeModeSelected(mode)`, com handler exaustivo `onAppearanceEvent` (steering, item UiEvent). Sem `@Suppress` nem `else ->`.
- `OnThemeModeSelected` → `viewModelScope.launch(dispatcher) { themeRepository.set(mode) }`. O estado vem do `observe()`, sem atualização otimista separada. O `draft` não é tocado (R1.3).
- UI: `SettingsSectionTitle(R.string.settings_appearance_section)` + três `SettingsRadioRow` (já existe em `SettingsRows.kt`, usado pela antecedência e pelo adiamento), com tags `SETTINGS_THEME_OPTION_PREFIX + mode.name`, `selected` e `Role.RadioButton` agrupados em `selectableGroup()`.

## Strings
- `settings_appearance_section` = `Aparência`
- `settings_theme_system` = `Seguir o sistema`
- `settings_theme_light` = `Claro`
- `settings_theme_dark` = `Escuro`

## Testes
- `ThemeModeTest` (JVM): `fromStorageId` para os três ids, `null`, desconhecido e ids com caixa diferente → padrão; `storageId` estáveis (falha se alguém renomear).
- `ThemeResolutionTest` (JVM): tabela `isDark` com 3 modos × sistema claro/escuro.
- `ThemePreferenceRepositoryImplTest` (DataStore em arquivo temporário, padrão da spec 06):
  - sem valor → `DARK`;
  - `set(LIGHT)` → `observe()` emite `LIGHT`;
  - **nova instância lendo o mesmo arquivo** → `LIGHT` (persistência, R2.2). Cancelar o escopo da primeira instância antes de criar a segunda: duas ativas no mesmo arquivo falham ("multiple DataStores active", lição da spec 06);
  - valor desconhecido gravado à mão → `DARK`;
  - `IOException` na leitura → `DARK`;
  - `set` não altera as chaves de `UserSettings` nem de `HomePreferences`.
- `ColorContrastTest` (JVM): calcula o contraste WCAG dos pares do R4.1 nas **duas** paletas e falha abaixo de 4,5:1. Isso protege mudanças futuras de cor.
- `SettingsViewModelTest` (acréscimos): `OnThemeModeSelected(LIGHT)` grava no repositório fake e o estado passa a `LIGHT`; um e-mail editado e não salvo continua no `draft` e não é gravado (R1.3).
- `SettingsScreenRobolectricTest` (acréscimos): a seção Aparência existe, com três opções `Role.RadioButton` e a atual `selected`; o clique envia o evento.
- `ThemeRobolectricTest` (`MainActivity`, grafo real, `-w411dp-h891dp`):
  1. sem preferência → tema escuro: `WindowInsetsControllerCompat(window, decorView).isAppearanceLightStatusBars == false`;
  2. Configurações → Claro → **sem recriar** a Activity: `isAppearanceLightStatusBars == true` e o back stack intacto (voltar leva à Home);
  3. `ActivityScenario.recreate()` e fechar/abrir uma nova `ActivityScenario` → continua claro (persistência);
  4. Seguir o sistema + `@Config(qualifiers = "night")` → escuro; sem `night` → claro.

  A cor desenhada não é verificada por pixel, porque `captureToImage` expira no Robolectric deste projeto (spec 09). O teste observável é o estilo das barras do sistema e o `background` do `MaterialTheme` lido por uma sonda de teste dentro do conteúdo.
- `FullScreenAlarmActivityRobolectricTest` (acréscimo): com `LIGHT` gravado, `isAppearanceLightStatusBars == true`.

## Melhorias futuras (fora do escopo)
- **`UiModeManager.setApplicationNightMode` (API 31+):** o sistema persiste o modo noturno da app e aplica o qualificador `-night` desde a partida, inclusive na Splash, o que permitiria uma Splash clara para quem escolheu Claro. Não entra agora por três motivos:
  1. causa mudança de configuração (recriação da Activity) a cada troca, a menos que se trate `uiMode` no manifest;
  2. não achamos na documentação oficial um valor que signifique "seguir o sistema" para a app;
  3. em API < 31 seria preciso outro caminho.
- Tema no arquivo de exportação da spec 17.
