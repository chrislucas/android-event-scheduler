# 03 — Configurações — Requisitos

## Introdução
Preferências do usuário persistidas em DataStore: conta ativa (e-mail), antecedência padrão do lembrete, tempo padrão de adiamento e alerta em tela cheia para eventos. A leitura é compartilhada (`core.preferences`) por Upsert, alarmes e Home; a tela de edição é `feature.settings`.

## Requisitos

### R1 — Preferências compartilhadas (`core.preferences`)
1. `UserSettings` SHALL conter: `activeAccountEmail` (padrão `christoffer.luccas@gmail.com`), `defaultReminderOffset: ReminderOffset` (padrão 15 min), `defaultSnoozeMinutes: Int` (padrão 10; permitidos 5, 10, 15), `fullScreenEventAlertEnabled: Boolean` (padrão `true`).
2. `UserSettingsRepository` SHALL oferecer `observe(): Flow<UserSettings>`, `current(): UserSettings` (suspend, primeira leitura) e `save(settings)`.
3. WHEN nada foi salvo THEN os valores padrão SHALL ser emitidos.
4. WHEN o DataStore estiver corrompido/ilegível THEN o repositório SHALL emitir os padrões (sem crash).
5. Persistência SHALL usar `DataStore<Preferences>` único do app (`core.datastore`), com chaves nomeadas e estáveis (`active_account_email`, `default_reminder_minutes`, `default_snooze_minutes`, `full_screen_event_alert`).

### R2 — Validação
1. `UserSettings.validate()` SHALL falhar com `InvalidEmail` quando o e-mail (após `trim`) estiver vazio ou não casar com padrão `local@dominio.tld`, e com `InvalidSnooze` quando fora de {5, 10, 15}.
2. O e-mail SHALL ser salvo já com `trim`.

### R3 — Tela de configurações
1. A tela SHALL mostrar: campo de e-mail (`SETTINGS_ACCOUNT_EMAIL_INPUT`), seletor de antecedência padrão (rádio: Sem alerta / 10 / 15 / 30 min / 1 h), seletor de adiamento (5 / 10 / 15 min), *switch* de alerta em tela cheia para eventos e botão "Salvar" (`SETTINGS_SAVE_BUTTON`).
2. WHEN a tela abre THEN os campos SHALL ser preenchidos com as preferências atuais **uma única vez** (edições do usuário não são sobrescritas por reemissões do DataStore).
3. WHEN o usuário toca Salvar com dados válidos THEN o app SHALL persistir, exibir confirmação e voltar.
4. WHEN inválido THEN SHALL mostrar o erro no campo (`isError` + texto de apoio) e NÃO navegar.
5. WHEN o usuário volta com alterações não salvas THEN o app SHALL descartar sem confirmação (comportamento simples; fora do escopo um diálogo).
6. Todos os controles SHALL ter semântica (`Role.RadioButton`, `Role.Switch`, `contentDescription`).

### R4 — Entrada na tela
1. A rota `Settings` SHALL ser aberta pela gaveta da Home (spec 06); esta spec expõe `settingsEntry(onBack)`.
