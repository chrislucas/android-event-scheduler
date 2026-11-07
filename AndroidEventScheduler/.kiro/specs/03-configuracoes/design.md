# 03 — Configurações — Design

## Pacotes
```
core/datastore/        DataStoreModule.kt   # DataStore<Preferences> "remember_me_prefs" (preferencesDataStore/ PreferenceDataStoreFactory)
core/preferences/
├── domain/model/      UserSettings.kt, SettingsValidationError (sealed)
├── domain/repository/ UserSettingsRepository.kt
├── data/              UserSettingsRepositoryImpl.kt (internal)  # lê/escreve DataStore + mapper de chaves
└── di/                PreferencesModule.kt

feature/settings/
├── presentation/{state,event,effect,viewmodel,screen,component}/
├── navigation/SettingsEntry.kt
└── di/SettingsModule.kt
```

## Domínio
```kotlin
data class UserSettings(
    val activeAccountEmail: String = DEFAULT_ACCOUNT_EMAIL,
    val defaultReminderOffset: ReminderOffset = ReminderOffset.FIFTEEN_MINUTES,
    val defaultSnoozeMinutes: Int = DEFAULT_SNOOZE_MINUTES,
    val fullScreenEventAlertEnabled: Boolean = true,
) {
    fun validate(): Result<UserSettings>   // trim do e-mail + regras R2; falha com SettingsValidationError
    companion object { const val DEFAULT_ACCOUNT_EMAIL = "christoffer.luccas@gmail.com"; val ALLOWED_SNOOZE_MINUTES = listOf(5, 10, 15) }
}
sealed class SettingsValidationError : Exception() { data object InvalidEmail; data object InvalidSnooze }
interface UserSettingsRepository {
    fun observe(): Flow<UserSettings>
    suspend fun current(): UserSettings = observe().first()
    suspend fun save(settings: UserSettings)
}
```
E-mail: regex simples e sem Android (`^[^@\s]+@[^@\s]+\.[^@\s]+$`) — domínio puro.

## Dados
- `observe()` = `dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.map(::toDomain)`.
- `save()` = `dataStore.edit { … }`. `ReminderOffset` persistido como minutos (`-1`/ausente = `NONE`).
- Instância do `DataStore` é `single` no Koin (`androidContext()`); em testes usar `PreferenceDataStoreFactory.create(scope, produceFile = tmp)`.

## Presentation
```kotlin
sealed class SettingsUiState {
    data object Loading : SettingsUiState()
    data class Content(val draft: UserSettings, val isSaving: Boolean = false, val emailError: Boolean = false) : SettingsUiState()
}
sealed interface SettingsUiEvent { OnEmailChanged(String); OnReminderSelected(ReminderOffset); OnSnoozeSelected(Int);
    OnFullScreenToggled(Boolean); OnSaveClicked; OnBackClicked }
sealed interface SettingsUiEffect { NavigateBack; ShowMessage(@StringRes Int) }
```
- `init`: `viewModelScope.launch(dispatcher) { _uiState.value = SettingsUiState.Content(draft = repository.current()) }` (leitura única). Edições atualizam `Content` via `updateContent { it.copy(...) }` (helper privado do VM).
- `OnSaveClicked`: `draft.validate()` → falha: `Content.emailError = true` (e `ShowMessage` para snooze); sucesso: `repository.save(it)` em `runCatching`, `ShowMessage(settings_saved)` + `NavigateBack`; erro de I/O: `ShowMessage(settings_save_error)`.
- Tela: `Scaffold` com `TopAppBar` (voltar), `Column` rolável; componentes `ReminderOptionRow`/`SnoozeOptionRow` reaproveitam o mesmo `RadioOptionRow` privado; previews: padrão, e-mail inválido, salvando.
- Tags novas: `SETTINGS_ACCOUNT_EMAIL_INPUT`, `SETTINGS_REMINDER_OPTION_PREFIX`, `SETTINGS_SNOOZE_OPTION_PREFIX`, `SETTINGS_FULLSCREEN_SWITCH`, `SETTINGS_SAVE_BUTTON`.

## Decisões de UseCase
Nenhum. `validate()` é regra de um único modelo, testável como método de domínio; `observe/current/save` são passa-direto. (O PDF tinha `ObserveUserSettingsUseCase`/`UpdateUserSettingsUseCase` — descartados por serem delegação pura.)

## Testes
- `UserSettingsTest`: e-mail válido/inválido/com espaços, snooze fora da lista, padrões.
- `UserSettingsRepositoryImplTest`: padrões quando vazio, round-trip, arquivo corrompido → padrões.
- `SettingsViewModelTest`: carrega uma única vez (reemissão do repo não sobrescreve edição); salvar inválido não emite `NavigateBack`; salvar válido persiste e emite `NavigateBack`; falha de I/O emite mensagem.
- `SettingsScreenRobolectricTest`: digitar e-mail e salvar dispara evento; rádio selecionado reflete `selected`; switch tem `Role.Switch`.
