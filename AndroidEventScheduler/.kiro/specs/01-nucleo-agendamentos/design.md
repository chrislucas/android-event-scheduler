# 01 — Núcleo de agendamentos — Design

## Pacotes
```
core/schedule/
├── domain/
│   ├── model/      ScheduleItem, ScheduleType, CalendarSource, ReminderOffset, RecurrencePattern,
│   │               ScheduleOccurrence, EventColorOption, StickerEmoji
│   ├── recurrence/ RecurrenceEngine
│   └── repository/ ScheduleRepository
├── data/
│   ├── local/      ScheduleEntity, ScheduleDao (o repositório usa o DAO direto)
│   ├── mapper/     ScheduleMapper (Entity↔Domain), RecurrenceMapper
│   └── repository/ ScheduleRepositoryImpl (internal)
└── di/             ScheduleCoreModule
core/database/      RememberMeDatabase (@Database), DatabaseModule (Room.databaseBuilder, DAO)
core/ui/format/     RecurrenceFormat.kt, ReminderFormat.kt, DateTimeFormat.kt
```
Sem camada `presentation` aqui. O `RememberMeDatabase` fica em `core.database` (infra), o DAO/entidade em `core.schedule.data` (pertence ao agregado); o `@Database` referencia a entidade — dependência de infra → agregado é aceitável porque ambos são `core`.

## Modelos (resumo)
```kotlin
data class ScheduleItem(
    val id: String, val title: String, val type: ScheduleType,
    val startDate: LocalDate, val endDate: LocalDate = startDate,
    val startTime: LocalTime, val endTime: LocalTime? = null, val isAllDay: Boolean = false,
    val location: String = "", val notes: String = "",
    val colorHex: Long = EventColorOption.LIGHT_BLUE.hex,
    val calendarSource: CalendarSource = CalendarSource.defaultFor(type),
    val reminderOffset: ReminderOffset = ReminderOffset.FIFTEEN_MINUTES,
    val recurrencePattern: RecurrencePattern = RecurrencePattern.None,
    val stickerEmoji: String? = null,
)
data class ScheduleOccurrence(val item: ScheduleItem, val startDate: LocalDate, val endDate: LocalDate)
sealed interface RecurrencePattern {
    data object None : RecurrencePattern
    data class Daily(val occurrenceLimit: Int? = null) : RecurrencePattern
    data class Weekly(val daysOfWeek: Set<DayOfWeek>, val occurrenceLimit: Int? = null) : RecurrencePattern
}
```
`Weekly.init` valida `daysOfWeek.isNotEmpty()` e limite ≥ 1 (`require`); `Daily` valida limite.

## RecurrenceEngine
```kotlin
class RecurrenceEngine {
    fun occurrenceDates(item: ScheduleItem, onOrAfter: LocalDate = item.startDate): Sequence<LocalDate>
    fun expand(item: ScheduleItem, from: LocalDate, to: LocalDate): List<ScheduleOccurrence>
}
```
Algoritmo (nomes sugestivos, sem `a/b/t`):
- `seriesDates(item)` gera datas candidatas desde `item.startDate` (Daily: +1 dia; Weekly: avança dia a dia filtrando `dayOfWeek in daysOfWeek`) e aplica `.take(limit)` quando houver limite → o índice da ocorrência é contado desde o início, nunca de `onOrAfter`.
- `occurrenceDates` = `seriesDates.dropWhile { it < onOrAfter }`.
- `expand`: `takeWhile { occurrenceStart <= to }`, filtra `occurrenceEnd >= from`; `occurrenceEnd = occurrenceStart + durationDays`.
- Janelas de exibição são pequenas (mês); para séries infinitas o custo é O(dias desde o início até `to`) — aceitável. Se medir lento, otimizar `Daily` com aritmética (`ChronoUnit.DAYS`) sem mudar a API.

## Room
```kotlin
@Entity(tableName = "schedules", indices = [Index("startDateEpochDay")])
data class ScheduleEntity(
    @PrimaryKey val id: String, val title: String, val type: String,
    val startDateEpochDay: Long, val endDateEpochDay: Long,
    val startMinuteOfDay: Int, val endMinuteOfDay: Int?, val isAllDay: Boolean,
    val location: String, val notes: String, val colorHex: Long, val calendarSourceId: String,
    val reminderMinutes: Int?, val recurrenceType: String, val recurrenceDaysMask: Int,
    val occurrenceLimit: Int?, val stickerEmoji: String?,
    val searchText: String,        // SearchTextNormalizer.normalize("$title $notes $location")
)
```
- `recurrenceDaysMask`: bit `dayOfWeek.value - 1` (segunda = bit 0).
- Queries:
  - `observeInRange`: `WHERE startDateEpochDay <= :to AND (recurrenceType != 'NONE' OR endDateEpochDay >= :from) ORDER BY startDateEpochDay, startMinuteOfDay`.
  - `search`: `WHERE searchText LIKE '%' || :normalizedQuery || '%' ESCAPE '\' AND (:type IS NULL OR type = :type) ORDER BY startDateEpochDay, startMinuteOfDay`. O repositório normaliza a consulta com `SearchTextNormalizer` e escapa `%`, `_` e `\` antes de chamar o DAO.
  - `SearchTextNormalizer` (`core.schedule.domain.search`, puro): `Normalizer.normalize(text, NFD)` → remove marcas diacríticas (`\p{Mn}`) → `lowercase(Locale.ROOT)` → colapsa espaços.
- `ScheduleDao.upsert` = `@Upsert`.
- Banco: `exportSchema = true`, `room.schemaLocation` em `app/schemas`. Testes de DAO com `Room.inMemoryDatabaseBuilder` + Robolectric.

## Repositório
```kotlin
interface ScheduleRepository {
    fun observeOccurrences(from: LocalDate, to: LocalDate): Flow<List<ScheduleOccurrence>>
    fun search(query: String, type: ScheduleType? = null): Flow<List<ScheduleItem>>
    suspend fun getById(id: String): ScheduleItem?
    fun observeById(id: String): Flow<ScheduleItem?>
    suspend fun getAll(): List<ScheduleItem>
    suspend fun upsert(item: ScheduleItem)
    suspend fun delete(id: String)
}
```
Métodos suspend lançam em falha de I/O; quem chama (VM/UseCase) usa `runCatching`. `ScheduleRepositoryImpl(dao, engine, ioDispatcher)` com `flowOn`. Filtro por `type` aplicado no SQL (`AND (:type IS NULL OR type = :type)`).

## Formatação (apresentação, `core.ui.format`)
Funções `fun RecurrencePattern.toDisplayLabel(resources: Resources): String` (puras sobre `Resources`, testáveis com Robolectric). Strings: `recurrence_none`, `recurrence_daily`, `recurrence_weekly_on` (`Toda semana em %1$s`), `plurals recurrence_times` (`%d vez`/`%d vezes`), abreviações de dia via `DayOfWeek.getDisplayName(TextStyle.SHORT, Locale("pt","BR"))` (garantir saída `ter.`, `qui.`).

## Desvios na execução

- **Rótulos fora do domínio**: `StickerEmoji` e `EventColorOption` não carregam texto (só emoji/hex). Os rótulos de acessibilidade ("Foguete", "Azul claro") vão para `strings.xml` na spec 09, seguindo `arquitetura.md` §5.5 (domínio não conhece `R`). O mesmo vale para `CalendarSource` (rótulo na spec 06).
- `ScheduleItem` valida `endDate >= startDate` no `init` (invariante de domínio).
- Utilitários acrescentados: `CalendarSource.fromId`, `ReminderOffset.fromMinutes`/`DEFAULT` (usados pelo mapper e pelas specs 03/17).
- Ordenação: no mesmo dia, itens de **dia inteiro primeiro**, depois por horário (DAO e `observeOccurrences`), já atendendo a spec 05 R4.2.
- `core.ui.format` usa `AppLocale` fixo (`pt-BR`), porque os textos do app só existem em pt-BR; os testes rodam com `@Config(qualifiers = "pt-rBR")`.
- Formatos conferidos na JVM (CLDR): `qui., 5 de nov., 22:00`, `ter.`, `qui.`. **Não verificado no ICU do Android em aparelho** — revalidar no marco M1.
- Testes de `Flow` do Room: a invalidação roda em thread própria; coletar em `backgroundScope.launch(Dispatchers.Default)` e receber por `Channel` (tempo virtual não controla o Room). Registrado no steering §5.2.

## Decisões de UseCase
Nenhum. `ScheduleRepository` é consumido direto pelos ViewModels (passa-direto). A expansão de recorrência é responsabilidade do repositório + engine (regra de domínio reutilizada por alarmes e home — o engine é injetado, não um UseCase).

## Testes mínimos
- `RecurrenceEngineTest`: diário sem limite; diário com limite 3; semanal ter/qui; semanal cuja `startDate` não bate o dia (1ª ocorrência avança); limite 5000 não estoura; multi-dia preserva duração; `expand` em fronteira de mês; `onOrAfter` no meio da série respeita contagem desde o início.
- `SearchTextNormalizerTest`: acentos, maiúsculas, espaços, `ç`, emoji preservado.
- `ScheduleMapperTest` ida e volta com todos os padrões (máscara de dias).
- `ScheduleDaoTest` (Robolectric): intervalo (evento fora/dentro/atravessando mês, recorrente iniciado antes), busca `reuniao` encontrando `REUNIÃO` em título/notas/local, escape de `%` e `_`, filtro por tipo, upsert substitui, delete.
- `ScheduleRepositoryImplTest`: reemite ao inserir; expande recorrência.
- `DisplayLabelTest`: `Toda semana em ter., qui. (5000 vezes)`, `Diariamente`, `Não repetir`, `1 vez`.
