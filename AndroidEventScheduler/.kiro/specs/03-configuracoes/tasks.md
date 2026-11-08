# 03 — Configurações — Tarefas

- [x] 1. `core.datastore`: módulo com `DataStore<Preferences>` único (R1.5).
- [x] 2. Domain: `UserSettings`, `SettingsValidationError`, `UserSettingsRepository` + `UserSettingsTest` (TDD) (R1, R2).
- [x] 3. Data: `UserSettingsRepositoryImpl` + `PreferencesModule` + `UserSettingsRepositoryImplTest` (incl. corrupção) (R1.3–R1.4).
- [x] 4. Presentation: contrato State/Event/Effect (sealed) + `SettingsViewModel` + `SettingsViewModelTest` (R3.2–R3.4).
- [x] 5. Presentation: `SettingsScreen`/`SettingsContent`, componentes, strings pt-BR, tags, previews, `SettingsScreenRobolectricTest` (R3.1, R3.6).
- [x] 6. `SettingsModule` + `settingsEntry(onBack)`; registrar em `AppModules`/`AppNavHost` (rota `Settings`).
- [x] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `UserSettingsTest`: e-mail válido/ inválido/ com espaços, snooze fora de {5,10,15}, valores padrão.
- [x] `UserSettingsRepositoryImplTest`: padrões quando vazio, round-trip, arquivo corrompido → padrões sem crash.
- [x] `SettingsViewModelTest`: carrega uma única vez (reemissão não sobrescreve edição); inválido não navega; válido persiste e emite `NavigateBack`.
- [x] `SettingsScreenRobolectricTest`: campo e-mail, rádios com `selected`, switch com `Role.Switch`, botão Salvar.
- [x] Rota `Settings` registrada (`settingsEntry` no `AppNavHost`); nenhuma feature importa `feature.settings` (conferido por busca); fluxo ponta a ponta tela → ViewModel → DataStore real (`SettingsFlowRobolectricTest`). A abertura pela gaveta da Home é da spec 06.
