# 03 — Configurações — Tarefas

- [ ] 1. `core.datastore`: módulo com `DataStore<Preferences>` único (R1.5).
- [ ] 2. Domain: `UserSettings`, `SettingsValidationError`, `UserSettingsRepository` + `UserSettingsTest` (TDD) (R1, R2).
- [ ] 3. Data: `UserSettingsRepositoryImpl` + `PreferencesModule` + `UserSettingsRepositoryImplTest` (incl. corrupção) (R1.3–R1.4).
- [ ] 4. Presentation: contrato State/Event/Effect (sealed) + `SettingsViewModel` + `SettingsViewModelTest` (R3.2–R3.4).
- [ ] 5. Presentation: `SettingsScreen`/`SettingsContent`, componentes, strings pt-BR, tags, previews, `SettingsScreenRobolectricTest` (R3.1, R3.6).
- [ ] 6. `SettingsModule` + `settingsEntry(onBack)`; registrar em `AppModules`/`AppNavHost` (rota `Settings`).
- [ ] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `UserSettingsTest`: e-mail válido/ inválido/ com espaços, snooze fora de {5,10,15}, valores padrão.
- [ ] `UserSettingsRepositoryImplTest`: padrões quando vazio, round-trip, arquivo corrompido → padrões sem crash.
- [ ] `SettingsViewModelTest`: carrega uma única vez (reemissão não sobrescreve edição); inválido não navega; válido persiste e emite `NavigateBack`.
- [ ] `SettingsScreenRobolectricTest`: campo e-mail, rádios com `selected`, switch com `Role.Switch`, botão Salvar.
- [ ] Rota `Settings` registrada (`settingsEntry`); nenhuma feature importa `feature.settings`.
