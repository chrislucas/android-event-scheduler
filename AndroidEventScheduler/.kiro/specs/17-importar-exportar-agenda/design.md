# 17 — Importar e exportar a agenda — Design

## Visão geral
```
Exportar:  BackupViewModel ─► AgendaBackupRepository.export(target) ─► ScheduleRepository.getAll()
                                    │                                  └► AgendaJsonCodec.encode()
                                    ├─ Share  ─► cacheDir/exports/*.json ─► FileProvider URI ─► UiEffect.ShareFile ─► share sheet
                                    └─ Save   ─► URI do SAF (CreateDocument) ─► ContentResolver.openOutputStream

Importar:  URI (OpenDocument | ACTION_VIEW/SEND) ─► AgendaBackupRepository.readForImport(uri)
              └► limite 5 MB ─► AgendaJsonCodec.decode() ─► validação ─► ImportPreview
           confirmação ─► ImportAgendaUseCase(preview, strategy)
              └► ScheduleRepository.upsertAll(items)   (transação)
              └► ScheduleNextAlarmUseCase(item) p/ cada item   (best-effort)
```

## Pacotes
```
feature/backup/
├── domain/
│   ├── model/      ImportPreview, ImportStrategy, ImportResult, ExportTarget, ExportResult, BackupError (sealed)
│   ├── repository/ AgendaBackupRepository (interface)
│   └── usecase/    ImportAgendaUseCase
├── data/
│   ├── dto/        AgendaFileDto, ScheduleItemDto, RecurrenceDto  (@Serializable)
│   ├── mapper/     AgendaDtoMapper (Dto ↔ ScheduleItem, com validação e truncamento)
│   ├── codec/      AgendaJsonCodec (kotlinx-serialization, Json { ignoreUnknownKeys = true })
│   ├── file/       ExportFileStore (cacheDir/exports, limpeza), DocumentStreams (ContentResolver)
│   └── repository/ AgendaBackupRepositoryImpl (internal)
├── presentation/{state,event,effect,viewmodel,component,screen}/
├── navigation/     BackupEntry.kt
└── di/             BackupModule.kt
core/schedule/ … ScheduleRepository.upsertAll(items)  (novo, com @Transaction no DAO)
core/navigation/ … rota AgendaBackup(pendingImportUri: String? = null)
```
`feature.backup` não importa `feature.settings`: o item de Configurações navega pela rota `AgendaBackup` (lambda injetada pelo `AppNavHost`).

## Domínio
```kotlin
enum class ImportStrategy { REPLACE_EXISTING, KEEP_EXISTING, IMPORT_AS_COPIES }

data class ImportPreview(
    val validItems: List<ScheduleItem>,
    val newCount: Int,
    val existingCount: Int,
    val invalidCount: Int,
    val existingIds: Set<String>,
)
data class ImportResult(val importedCount: Int, val skippedCount: Int, val alarmFailures: Int)

sealed class BackupError : Exception() {
    data object NotRememberMeFile : BackupError()
    data class UnsupportedVersion(val version: Int) : BackupError()
    data object Corrupted : BackupError()
    data object TooLarge : BackupError()
    data object EmptyAgenda : BackupError()
    data object Io : BackupError()
}

sealed interface ExportTarget { data object ShareCache : ExportTarget; data class Document(val uri: String) : ExportTarget }
data class ExportResult(val uri: String, val fileName: String, val itemCount: Int)

interface AgendaBackupRepository {
    suspend fun countItems(): Int
    suspend fun export(target: ExportTarget): ExportResult            // lança BackupError
    suspend fun readForImport(uri: String): ImportPreview             // lança BackupError
}
```
`uri` como `String` mantém o domínio sem `android.net.Uri`.

### `ImportAgendaUseCase` (justificado)
```kotlin
class ImportAgendaUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(preview: ImportPreview, strategy: ImportStrategy, copySuffix: String): Result<ImportResult>
}
```
- Seleção por estratégia: `REPLACE_EXISTING` → todos; `KEEP_EXISTING` → só `id ∉ existingIds`; `IMPORT_AS_COPIES` → existentes ganham novo id e `title + copySuffix` (sufixo vindo do `StringProvider`, como na spec 11).
- `scheduleRepository.upsertAll(selected)` numa transação (R6.1); depois `runCatching { scheduleNextAlarm(item) }` para cada um, contando falhas (R6.2).
- **Por que UseCase:** orquestra banco + alarmes com semântica de falha parcial e contém a regra das estratégias (critérios 2 e 3 da política). A exportação **não** tem UseCase: é leitura + serialização, responsabilidade do repositório.

## Dados
- **Codec**: `Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }`. `ignoreUnknownKeys` permite ler arquivos de versões futuras compatíveis; `version > 1` → `UnsupportedVersion`.
- **Validação em duas camadas**: (1) estrutural — falha de parse/cabeçalho → erro do arquivo inteiro; (2) por item no `AgendaDtoMapper.toDomainOrNull()` — erro → item contado como inválido. Datas via `LocalDate.parse`, horas `LocalTime.parse`, enums por `entries.firstOrNull { it.name == value }`. Construtores do domínio que lançam (`Weekly` vazio etc.) ficam dentro de `runCatching`.
- **Limite de tamanho**: `ContentResolver.query(OpenableColumns.SIZE)` quando disponível; e leitura por `inputStream.buffered()` com contador que aborta ao passar de 5 MB (R4.3) — vale mesmo quando o provedor não informa o tamanho.
- **`ExportFileStore`**: `File(cacheDir, "exports")`, apaga arquivos anteriores, grava `rememberme-agenda-<data>.json`, devolve `FileProvider.getUriForFile(context, "${packageName}.fileprovider", file)`.
- **Manifest**: `<provider androidx.core.content.FileProvider authorities="${applicationId}.fileprovider" exported="false" grantUriPermissions="true">` + `res/xml/file_paths.xml` com `<cache-path name="exports" path="exports/"/>`.
- **Receber arquivos**: na `MainActivity`, `intent-filter` para `VIEW` (`application/json`, categorias `DEFAULT`/`BROWSABLE`) e `SEND` (`application/json`). `onCreate/onNewIntent` → `IncomingFileState` (como o `DeepLinkState` da spec 04) → o `AppNavHost` abre `AgendaBackup(pendingImportUri)` quando desbloqueado. A permissão de leitura da URI recebida vale enquanto a Activity vive; a leitura acontece logo ao abrir a tela (pré-visualização) e o conteúdo validado fica em memória até confirmar.

## Presentation
```kotlin
sealed class BackupUiState {
    data object Loading : BackupUiState()
    data class Content(
        val itemCount: Int,
        val isExporting: Boolean = false,
        val isReadingImport: Boolean = false,
        val preview: ImportPreview? = null,
        val strategy: ImportStrategy = ImportStrategy.REPLACE_EXISTING,
        val isImporting: Boolean = false,
    ) : BackupUiState()
}
sealed interface BackupUiEvent {
    data object OnShareExportClicked; data object OnSaveExportClicked
    data class OnSaveDocumentCreated(val uri: String?)          // retorno do CreateDocument
    data object OnPickImportClicked
    data class OnImportFileChosen(val uri: String?)             // retorno do OpenDocument ou arquivo recebido
    data class OnStrategySelected(val strategy: ImportStrategy)
    data object OnConfirmImportClicked; data object OnCancelImportClicked; data object OnBackClicked
}
sealed interface BackupUiEffect {
    data class ShareFile(val uri: String, val subject: String, val body: String) : BackupUiEffect
    data class LaunchCreateDocument(val suggestedName: String) : BackupUiEffect
    data object LaunchOpenDocument : BackupUiEffect
    data class ShowMessage(@StringRes val messageRes: Int, val formatArgs: List<Any> = emptyList()) : BackupUiEffect
    data object NavigateBack : BackupUiEffect
}
```
- `BackupViewModel(pendingImportUri, backupRepository, importAgendaUseCase, stringProvider, clock, dispatcher)`; com `pendingImportUri != null` chama `readForImport` no `init`.
- A tela registra `rememberLauncherForActivityResult(CreateDocument("application/json"))` e `OpenDocument()`; efeitos `LaunchCreateDocument`/`LaunchOpenDocument` disparam os launchers (chamando `appLockController.suppressNextLock()`, spec 16, se presente) e devolvem `OnSaveDocumentCreated`/`OnImportFileChosen`.
- `ShareFile` → `Intent(ACTION_SEND).setType("application/json").putExtra(EXTRA_STREAM, uri).putExtra(EXTRA_SUBJECT, …).putExtra(EXTRA_TEXT, …).addFlags(FLAG_GRANT_READ_URI_PERMISSION)` + `createChooser`; `ClipData` com a URI (necessário para a permissão chegar em alguns apps de e-mail). Mesmo padrão de robustez da spec 12 (`ActivityNotFoundException` → mensagem).
- Componentes: `ExportSection`, `ImportSection`, `ImportPreviewCard`, `ImportStrategySelector` — previews: vazio, com itens, exportando, pré-visualização com repetidos/inválidos, importando.

## Decisões de UseCase
- `ImportAgendaUseCase`: justificado (orquestração banco + alarmes, regra das estratégias).
- Exportação e pré-visualização: sem UseCase (repositório).

## Testes mínimos
- `AgendaJsonCodecTest`: round-trip de todos os campos e padrões de recorrência; enums por nome; campo desconhecido ignorado; `format` errado; `version` 2; JSON truncado.
- `AgendaDtoMapperTest`: item inválido vira `null` (data ruim, `endDate < startDate`, Weekly vazio, enum desconhecido); truncamento de strings.
- `ImportAgendaUseCaseTest`: cada estratégia; transação chamada uma vez com os itens certos; alarme por item; falha de alarme não desfaz; falha do `upsertAll` → `Result.failure` e nenhum alarme; idempotência com `REPLACE_EXISTING`.
- `ScheduleDaoTest` (acréscimo): `upsertAll` atômico (falha no meio não grava nada).
- `AgendaBackupRepositoryImplTest` (Robolectric + Room em memória): export gera JSON válido com N itens e limpa o cache; import com arquivo > 5 MB → `TooLarge`; preview conta novos/existentes/inválidos.
- `BackupViewModelTest`: agenda vazia desabilita; share emite `ShareFile`; save cria documento e mostra mensagem; arquivo inválido mostra o motivo sem gravar; confirmar importa com a estratégia escolhida; toques repetidos ignorados; `pendingImportUri` abre direto na pré-visualização.
- `BackupScreenRobolectricTest`: seções, botões, aviso de criptografia, `Role.RadioButton` nas estratégias, contagens com `plurals`.
- `ShareFileIntentRobolectricTest`: chooser com `ACTION_SEND`, `application/json`, `EXTRA_STREAM`, assunto, texto e `FLAG_GRANT_READ_URI_PERMISSION`.
- `IncomingFileRobolectricTest`: `ACTION_VIEW` com `application/json` na `MainActivity` leva à pré-visualização.
- **Não verificável em JVM** (declarar no PR): envio real por um app de e-mail, seletor de arquivos real do sistema, abrir anexo a partir de outro app.
