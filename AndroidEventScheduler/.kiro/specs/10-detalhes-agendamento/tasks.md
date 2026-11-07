# 10 — Detalhes do agendamento — Tarefas

- [ ] 1. Confirmar `observeById` no `ScheduleRepository`/DAO/Impl (spec 01 R4.1); se faltar, implementar com teste.
- [ ] 2. Criar o `StringProvider` em `core.common` (interface + impl com `Context` + fake de teste; também usado pelas specs 11 e 12), depois `ScheduleDetailUi` + `ScheduleDetailMapper` + `ScheduleDetailMapperTest` (TDD) — validar abreviações pt-BR de dia/mês (R1.2).
- [ ] 3. `ScheduleDetailUiState/Event/Effect` e `ScheduleDetailViewModel` + `ScheduleDetailViewModelTest` (R2).
- [ ] 4. Componentes: `DetailInfoRow`, `ScheduleActionBar`/`ScheduleAction` com previews e tags (R1, R3.3, R4).
- [ ] 5. `ScheduleDetailScreen`/`Content` + `ScheduleDetailScreenRobolectricTest`.
- [ ] 6. `DetailModule` + `detailEntry`; ligar `AppNavHost` (Home/Pesquisa/notificação → Detail; Detail → Upsert).
- [ ] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `ScheduleDetailMapperTest`: todos os formatos de intervalo, rótulos de calendário, omissão de local/notas vazios, recorrência e alerta.
- [ ] `ScheduleDetailViewModelTest`: carrega; inexistente → mensagem + voltar; removido depois → voltar sem erro; edição externa atualiza; `OnEditClicked` → `NavigateToEdit`.
- [ ] `ScheduleDetailScreenRobolectricTest`: textos/tags/descrições; local e notas ausentes não existem; carregando.
- [ ] `StringProvider` criado em `core.common` com fake; Detail liga Home/notificação → Detail e Detail → Upsert; placeholders removidos.
