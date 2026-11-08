# 10 — Detalhes do agendamento — Tarefas

- [x] 1. Confirmar `observeById` no `ScheduleRepository`/DAO/Impl (spec 01 R4.1); se faltar, implementar com teste. (já existia e é coberto por `ScheduleRepositoryImplTest`/`ScheduleDaoTest`; nada a implementar)
- [x] 2. Criar o `StringProvider` em `core.common` (interface + impl com `Context` + fake de teste; também usado pelas specs 11 e 12), depois `ScheduleDetailUi` + `ScheduleDetailMapper` + `ScheduleDetailMapperTest` (TDD) — validar abreviações pt-BR de dia/mês (R1.2).
- [x] 3. `ScheduleDetailUiState/Event/Effect` e `ScheduleDetailViewModel` + `ScheduleDetailViewModelTest` (R2).
- [x] 4. Componentes: `DetailInfoRow`, `ScheduleActionBar`/`ScheduleAction` com previews e tags (R1, R3.3, R4).
- [x] 5. `ScheduleDetailScreen`/`Content` + `ScheduleDetailScreenRobolectricTest`.
- [x] 6. `DetailModule` + `detailEntry`; ligar `AppNavHost` (Home/Pesquisa/notificação → Detail; Detail → Upsert).
- [x] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `ScheduleDetailMapperTest`: todos os formatos de intervalo, rótulos de calendário, omissão de local/notas vazios, recorrência e alerta.
- [x] `ScheduleDetailViewModelTest`: carrega; inexistente → mensagem + voltar; removido depois → voltar sem erro; edição externa atualiza; `OnEditClicked` → `NavigateToEdit`.
- [x] `ScheduleDetailScreenRobolectricTest`: textos/tags/descrições; local e notas ausentes não existem; carregando.
- [x] `StringProvider` criado em `core.common` com fake.
- [x] Detail → Upsert ligado no `AppNavHost` (`onEdit` abre `UpsertSchedule(scheduleId)`); o placeholder de Upsert (spec 07 o remove) foi criado aqui. (verificado por compilação e `:app:assembleDebug`; não há teste do `NavDisplay` que clique em Editar e confira a abertura do placeholder)
- [ ] Home/pesquisa/notificação → Detail ligados (não verificado: essas saídas pertencem às specs 04, 05 e 13, ainda não mergeadas; esta spec entrega a `detailEntry` e a rota já abre o detalhe — coberto por `DetailFlowRobolectricTest` com a tela direta, sem passar pelo `NavDisplay`).
