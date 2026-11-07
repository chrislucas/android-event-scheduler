# 01 — Núcleo de agendamentos — Tarefas

- [ ] 1. Domain: criar modelos, enums e catálogos (`EventColorOption`, `StickerEmoji`) e a `ScheduleNotFoundException` (usada pelas specs 11 e 14) em `core.schedule.domain.model` (R1) + testes de invariantes (`Weekly` vazio falha, limite < 1 falha, `fromHex` fallback).
- [ ] 1b. Domain (TDD): `SearchTextNormalizer` + `SearchTextNormalizerTest` (R3.5).
- [ ] 2. Domain: implementar `RecurrenceEngine` (R2) e `RecurrenceEngineTest` (todos os cenários do design) — TDD: escreva os testes primeiro.
- [ ] 3. Domain: interface `ScheduleRepository` (R4.1).
- [ ] 4. Data: `ScheduleEntity`, `ScheduleDao`, `RememberMeDatabase`, schema exportado, `DatabaseModule` Koin (R3) + `ScheduleDaoTest`.
- [ ] 5. Data: `ScheduleMapper`/`RecurrenceMapper` + `ScheduleMapperTest`.
- [ ] 6. Data: `ScheduleRepositoryImpl` (`internal`) + `ScheduleCoreModule` registrado em `AppModules` + `ScheduleRepositoryImplTest`.
- [ ] 7. Presentation compartilhada: `core.ui.format` + strings pt-BR/plurals (R5) + `DisplayLabelTest`.
- [ ] 8. Atualizar `KoinModulesVerificationTest` com o novo módulo.
- [ ] 9. Verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `RecurrenceEngineTest` cobre: diário (com/sem limite), semanal ter/qui, 1ª ocorrência fora do dia marcado, multi-dia, `onOrAfter` no meio da série, fronteira de mês, limite 5000 — todos verdes.
- [ ] `ScheduleDaoTest` (Room em memória) cobre intervalo, recorrente iniciado antes, busca sem acento/caixa, escape de `%`/`_`, filtro por tipo, `observeById`, upsert e delete.
- [ ] `ScheduleRepositoryImplTest`: reemite ao mudar a tabela e expande recorrência em `observeOccurrences`.
- [ ] `DisplayLabelTest`: `Não repetir`, `Diariamente`, `Toda semana em ter., qui. (5000 vezes)`, `1 vez`, rótulos de alerta.
- [ ] `core.schedule.domain` sem imports de `android.*`; schema do Room exportado; sem migração destrutiva; `ScheduleNotFoundException` existe.
