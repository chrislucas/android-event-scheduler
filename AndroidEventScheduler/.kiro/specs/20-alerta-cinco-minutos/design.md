# 20 — Alerta 5 minutos antes — Design

## Visão geral

```
core/schedule/domain/model/ReminderOffset.kt   (alterado: FIVE_MINUTES(minutes = 5) entre NONE e TEN_MINUTES)
docs/formato-agenda-json.md                    (alterado: valor novo e compatibilidade com versões anteriores)
```

O resto do app já trabalha com `ReminderOffset.entries` e `minutes`, então a opção nova aparece e funciona sem outra
mudança de produção:

| Onde | Como usa `ReminderOffset` | Efeito da opção nova |
|---|---|---|
| `ReminderOffsetRow` (Upsert) | `OptionGroup(options = ReminderOffset.entries)` | Opção `5 minutos antes`, tag `UPSERT_REMINDER_OPTION_PREFIX + "FIVE_MINUTES"` |
| `SettingsScreen` | `SettingsRadioGroup(options = ReminderOffset.entries)` | Opção `5 minutos antes`, tag `SETTINGS_REMINDER_OPTION_PREFIX + "FIVE_MINUTES"` |
| `ReminderOffset.toDisplayLabel` (`core.ui.format`) | `minutes` → plural `reminder_minutes_before`; 60 → `1 hora antes` | `5 minutos antes` (detalhe, formulário, Configurações, texto compartilhado) |
| `ScheduleNextAlarmUseCase` | gatilho = início − `minutes` | 5 minutos antes |
| `ScheduleMapper` (Room) | grava `minutes`; lê com `fromMinutes` | `5` ↔ `FIVE_MINUTES` |
| `UserSettingsRepositoryImpl` (DataStore) | grava `minutes`; lê com `fromMinutes` | `5` ↔ `FIVE_MINUTES` |
| `AgendaDtoMapper` (backup) | grava `name`; lê com `enumByName` | `"FIVE_MINUTES"` ↔ `FIVE_MINUTES` |

Nenhuma permissão, rota, módulo Koin, string, tag, dependência ou UseCase novo. Nenhuma migração de banco.

## Decisões

| Decisão | Escolha | Alternativa rejeitada e motivo |
|---|---|---|
| Como oferecer 5 min | **Valor novo no enum** `ReminderOffset` | Antecedência livre (`Int` digitado): muda o modelo, o formulário, Configurações, o backup e a validação, e foge do pedido. O catálogo fechado mantém o formulário simples e o arquivo validável |
| Posição no enum | Entre `NONE` e `TEN_MINUTES` | No fim: a lista ficaria fora de ordem (`… 1 hora, 5 minutos`). O `ordinal` não é persistido em lugar nenhum (Room e DataStore guardam `minutes`, o arquivo guarda `name`), então a posição é livre |
| Versão do arquivo de agenda | **Continua `1`** | Ir para `2`: versões anteriores do app recusariam **todo** arquivo exportado, mesmo sem nenhum item com 5 min. Mantendo `1`, elas recusam só os itens com `FIVE_MINUTES` (regra "por item" da spec 17), e o resto é importado |
| Padrão | Continua 15 min | Trocar o padrão muda o comportamento de quem não pediu |
| Rótulo | Plural existente `reminder_minutes_before` | String própria: duplicaria o que o plural já resolve (`5 minutos antes`) |

### Decisões de UseCase
Nenhuma mudança: o `ScheduleNextAlarmUseCase` já calcula `início − minutes` para qualquer antecedência.

## Testes

| Requisito | Teste |
|---|---|
| R1.1 | `ScheduleModelsTest`: `ReminderOffset.entries.map { it.minutes } == [null, 5, 10, 15, 30, 60]` |
| R1.2 | `DisplayLabelTest`: rótulos pt-BR incluem `5 minutos antes`; `ScheduleDetailMapperTest`: `reminderText` com 5 min |
| R1.3 | `UserSettingsTest` e `AgendaDtoMapperTest` (padrão quando ausente) continuam verdes sem edição |
| R2.1 | `UpsertScheduleViewModelTest`: selecionar 5 min e salvar grava `FIVE_MINUTES`; `UpsertScheduleScreenRobolectricTest`: a opção existe e emite o evento |
| R2.2 | `UpsertScheduleViewModelTest`: edição de item com 5 min carrega `FIVE_MINUTES` |
| R2.3 | `SettingsScreenRobolectricTest`: a opção existe e emite o evento; `UpsertScheduleViewModelTest`: criação com padrão 5 min |
| R3.1 | `ScheduleNextAlarmUseCaseTest`: item único (09:55 para início 10:00) e dia inteiro (08:55) |
| R3.2 | `ScheduleNextAlarmUseCaseTest`: série diária alerta 5 min antes da próxima ocorrência |
| R3.3 | `ScheduleNextAlarmUseCaseTest`: início daqui a 3 min não agenda |
| R4.1 | `ScheduleMapperTest`: `5` ↔ `FIVE_MINUTES` |
| R4.2 | `UserSettingsRepositoryImplTest`: grava e lê `FIVE_MINUTES` |
| R4.3 | `AgendaDtoMapperTest`: importa `"FIVE_MINUTES"` e faz *round-trip* |
| R4.4 | `AgendaDtoMapperTest`: nome desconhecido (`"TWO_MINUTES"`) recusa o item |
| Ponta a ponta | `FiveMinuteReminderRobolectricTest` (com `MainActivity`): Home → criar → Alerta `5 minutos antes` → salvar → item gravado com `FIVE_MINUTES` e alarme em início − 5 min |
