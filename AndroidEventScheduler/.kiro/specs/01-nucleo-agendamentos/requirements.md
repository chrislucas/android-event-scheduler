# 01 — Núcleo de agendamentos — Requisitos

## Introdução
Base de dados e domínio compartilhados por Home, Upsert, Detail, Search e Alarmes: modelo `ScheduleItem`, persistência offline em Room, repositório e `RecurrenceEngine`. Vive em `core.schedule` porque várias features o usam e uma feature não pode importar outra.

## Requisitos

### R1 — Modelo de domínio
1. `ScheduleItem` SHALL conter: `id`, `title`, `type` (`EVENT`|`REMINDER`), `startDate`, `endDate` (≥ `startDate`), `startTime`, `endTime?`, `isAllDay`, `location`, `notes`, `colorHex`, `calendarSource`, `reminderOffset`, `recurrencePattern`, `stickerEmoji?`.
2. `CalendarSource` SHALL ser `PERSONAL_ACCOUNT`, `FAMILY`, `MY_REMINDERS`, com `id` estável e `colorHex`; o padrão derivado do tipo: `REMINDER → MY_REMINDERS`, `EVENT → PERSONAL_ACCOUNT`.
3. `ReminderOffset` SHALL ter `NONE`, `TEN_MINUTES`, `FIFTEEN_MINUTES` (padrão), `THIRTY_MINUTES`, `ONE_HOUR`, com `minutes: Int?`.
4. `RecurrencePattern` SHALL ser `None`, `Daily(occurrenceLimit: Int?)`, `Weekly(daysOfWeek: Set<DayOfWeek>, occurrenceLimit: Int?)` — `Weekly` exige ≥ 1 dia e `occurrenceLimit` (quando presente) ≥ 1.
5. `EventColorOption` (6 cores) e `StickerEmoji` (8 emojis com rótulo de acessibilidade em pt-BR) SHALL existir como catálogos de domínio; `EventColorOption.fromHex` devolve `LIGHT_BLUE` para hex desconhecido.
6. O pacote `core.schedule.domain` SHALL ser Kotlin/JVM puro (sem `android.*`).

### R2 — Motor de recorrência
1. O `RecurrenceEngine` SHALL ser puro e determinístico, sem relógio interno.
2. **Ocorrência**: a série começa em `startDate`; para `Daily` toda data ≥ `startDate`; para `Weekly` toda data ≥ `startDate` cujo dia da semana esteja no conjunto. A 1ª ocorrência é a primeira data que satisfaz o padrão.
3. WHEN `occurrenceLimit = N` THEN a série SHALL ter exatamente N ocorrências contadas desde a 1ª.
4. WHEN o item cobre vários dias (`endDate > startDate`) THEN cada ocorrência SHALL preservar a duração (`endDate' = data + (endDate − startDate)`).
5. `expand(item, from, to)` SHALL devolver `ScheduleOccurrence(item, startDate, endDate)` cujo intervalo intersecta `[from, to]`, ordenadas por data e hora; item sem recorrência gera no máximo uma.
6. `occurrenceDates(item, onOrAfter)` SHALL ser uma `Sequence` preguiçosa (usada pelos alarmes).

### R3 — Persistência
1. O banco Room `remember_me.db` SHALL ter a tabela `schedules` com o `ScheduleEntity` equivalente ao modelo (datas em epoch day, horas em minuto do dia, recorrência como tipo + máscara de dias + limite).
2. O schema SHALL ser exportado (`exportSchema = true`) e versionado; **sem** `fallbackToDestructiveMigration`.
3. O DAO SHALL oferecer: `observeInRange(from, to)`, `search(query)`, `getById`, `observeById`, `getAll`, `upsert`, `deleteById`.
4. A consulta de intervalo SHALL trazer itens sem recorrência que intersectam o intervalo **e** itens recorrentes com `startDate ≤ to`.
5. A busca SHALL ser insensível a maiúsculas **e a acentos** (`reuniao` encontra `Reunião`), por substring em título, notas e local, ordenada por data/hora; para isso a entidade guarda `searchText` normalizado, calculado ao gravar (o `LIKE ... NOCASE` do SQLite só cobre ASCII).

### R4 — Repositório
1. `ScheduleRepository` (interface no domínio) SHALL oferecer: `observeOccurrences(from, to): Flow<List<ScheduleOccurrence>>`, `search(query, type?): Flow<List<ScheduleItem>>`, `getById`, `observeById(id): Flow<ScheduleItem?>` (emite `null` se inexistente/removido), `getAll`, `upsert`, `delete`.
2. `observeOccurrences` SHALL aplicar o `RecurrenceEngine` sobre o resultado do DAO e reemitir quando a tabela mudar.
3. A implementação SHALL ser `internal` à camada de dados e ligada por Koin.

### R5 — Formatação compartilhada de apresentação
1. `core.ui.format` SHALL oferecer rótulos pt-BR: `RecurrencePattern.toDisplayLabel()` (`Não repetir`, `Diariamente`, `Toda semana em ter., qui. (5000 vezes)` — dias na ordem dom→sáb, sufixo ` (N vezes)` só com limite), `ReminderOffset.toDisplayLabel()` (`Sem alerta`, `10 minutos antes`…), e data/hora curta (`qui., 5 de nov., 22:00`).
2. Os textos SHALL vir de `strings.xml`/`plurals`.
