# 20 — Alerta 5 minutos antes — Tarefas

- [x] 1. TDD do domínio: `ScheduleModelsTest` (catálogo com 5 min) e `DisplayLabelTest` (`5 minutos antes`) primeiro; depois `ReminderOffset.FIVE_MINUTES` (R1.1, R1.2). Enum e testes no **mesmo commit**.
- [x] 2. Alarme: casos de 5 min no `ScheduleNextAlarmUseCaseTest` (item único, dia inteiro, série diária, início daqui a 3 min) (R3).
- [x] 3. Persistência: `ScheduleMapperTest` (Room) e `UserSettingsRepositoryImplTest` (DataStore) com `FIVE_MINUTES` (R4.1, R4.2).
- [x] 4. Arquivo de agenda: `AgendaDtoMapperTest` (aceita `FIVE_MINUTES`, *round-trip*, nome desconhecido recusado) e `docs/formato-agenda-json.md` (valor novo e compatibilidade) (R4.3, R4.4).
- [x] 5. Apresentação: `UpsertScheduleViewModelTest` (salvar, editar e padrão com 5 min), `UpsertScheduleScreenRobolectricTest` e `SettingsScreenRobolectricTest` (opção visível e evento), `ScheduleDetailMapperTest` (rótulo) (R1.2, R2).
- [x] 6. Ponta a ponta `FiveMinuteReminderRobolectricTest` com a `MainActivity`: criar com `5 minutos antes` → item gravado e alarme em início − 5 min (R2.1, R3.1).
- [x] 7. Verificação final conforme [`criterios-de-aceite.md`](../criterios-de-aceite.md), incluindo a checagem por commit; `ordem-de-execucao.md` §8 e `README.md` das specs atualizados.

## Critérios de aceite

A spec só é considerada concluída, e o PR só é aberto, quando **todos** estiverem marcados `[x]`, cada um verificado de fato e não presumido. Somam-se a eles os **critérios gerais** em [`.kiro/specs/criterios-de-aceite.md`](../criterios-de-aceite.md).

- [x] `5 minutos antes` aparece entre `Sem alerta` e `10 minutos antes` no formulário e em Configurações (testes de tela).
- [x] Salvar com 5 min grava `FIVE_MINUTES` e agenda o alarme em início − 5 min (ViewModel, use case e ponta a ponta).
- [x] Dia inteiro com 5 min alerta às 08:55; série diária alerta 5 min antes de cada ocorrência.
- [x] Padrão de Configurações em 5 min faz a criação abrir com 5 min.
- [x] Room, DataStore e arquivo de agenda fazem *round-trip* de `FIVE_MINUTES`; nome desconhecido continua recusado.
- [x] O padrão continua 15 min (testes existentes do padrão verdes **sem edição**).
- [x] Nenhuma migração, permissão, rota, módulo Koin, string, tag, dependência ou UseCase novo.
- [x] `docs/formato-agenda-json.md` documenta o valor novo e o comportamento de versões anteriores do app.
- [ ] Não verificado em aparelho, declarado no PR: notificação real chegando 5 minutos antes (não verificado: depende de aparelho/emulador; coberto por Robolectric com o `ShadowAlarmManager`).
