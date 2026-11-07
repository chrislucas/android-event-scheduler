# 01 — Núcleo de agendamentos — Tarefas

- [x] 1. Domain: criar modelos, enums e catálogos (`EventColorOption`, `StickerEmoji`) e a `ScheduleNotFoundException` (usada pelas specs 11 e 14) em `core.schedule.domain.model` (R1) + testes de invariantes (`Weekly` vazio falha, limite < 1 falha, `fromHex` fallback).
- [x] 1b. Domain (TDD): `SearchTextNormalizer` + `SearchTextNormalizerTest` (R3.5).
- [x] 2. Domain: implementar `RecurrenceEngine` (R2) e `RecurrenceEngineTest` (todos os cenários do design) — TDD: escreva os testes primeiro.
- [x] 3. Domain: interface `ScheduleRepository` (R4.1).
- [x] 4. Data: `ScheduleEntity`, `ScheduleDao`, `RememberMeDatabase`, schema exportado, `DatabaseModule` Koin (R3) + `ScheduleDaoTest`.
- [x] 5. Data: `ScheduleMapper`/`RecurrenceMapper` + `ScheduleMapperTest`.
- [x] 6. Data: `ScheduleRepositoryImpl` (`internal`) + `ScheduleCoreModule` registrado em `AppModules` + `ScheduleRepositoryImplTest`.
- [x] 7. Presentation compartilhada: `core.ui.format` + strings pt-BR/plurals (R5) + `DisplayLabelTest`.
- [x] 8. Atualizar `KoinModulesVerificationTest` com o novo módulo.
- [x] 9. Verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `RecurrenceEngineTest` cobre: diário (com/sem limite), semanal ter/qui, 1ª ocorrência fora do dia marcado, multi-dia, `onOrAfter` no meio da série, fronteira de mês, limite 5000 — todos verdes.
- [x] `ScheduleDaoTest` (Room em memória) cobre intervalo, recorrente iniciado antes, busca sem acento/caixa, escape de `%`/`_`, filtro por tipo, `observeById`, upsert e delete.
- [x] `ScheduleRepositoryImplTest`: reemite ao mudar a tabela e expande recorrência em `observeOccurrences`.
- [x] `DisplayLabelTest`: `Não repetir`, `Diariamente`, `Toda semana em ter., qui. (5000 vezes)`, `1 vez`, rótulos de alerta e `qui., 5 de nov., 22:00` (JVM/CLDR; ICU do aparelho não verificado).
- [x] `core.schedule.domain` sem imports de `android.*` (conferido por busca); schema do Room exportado (`app/schemas/.../1.json`); sem migração destrutiva; `ScheduleNotFoundException` existe (testada); grafo Koin real resolve e usa o repositório (`ScheduleCoreModuleRobolectricTest`).
