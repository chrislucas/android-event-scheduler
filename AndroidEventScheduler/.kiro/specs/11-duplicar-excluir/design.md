# 11 — Duplicar e excluir agendamento — Design

## Mudanças em `feature.detail`
```
domain/usecase/DuplicateScheduleUseCase.kt   (novo)
domain/usecase/DeleteScheduleUseCase.kt      (novo)
presentation/component/DeleteConfirmationDialog.kt (novo)
presentation/{state,event,effect,viewmodel,screen}    (alterados)
navigation/DetailEntry.kt                              (alterado: onOpenCopy)
```

## Domínio
```kotlin
class DuplicateScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val scheduleNextAlarm: ScheduleNextAlarmUseCase,
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
) {
    suspend operator fun invoke(sourceId: String, titleSuffix: String): Result<ScheduleItem> = runCatching {
        val original = scheduleRepository.getById(sourceId) ?: throw ScheduleNotFoundException(sourceId)
        val copy = original.copy(id = idProvider(), title = original.title + titleSuffix)
        scheduleRepository.upsert(copy)
        runCatching { scheduleNextAlarm(copy) }     // R1.6: alarme não desfaz a cópia
        copy
    }
}

class DeleteScheduleUseCase(
    private val scheduleRepository: ScheduleRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend operator fun invoke(scheduleId: String): Result<Unit> = runCatching {
        scheduleRepository.delete(scheduleId)             // 1º banco: se falhar, nada mais acontece (R2.5)
        runCatching { alarmScheduler.cancel(scheduleId) } // 2º alarme, best-effort (R2.6)
    }
}
```
- Sufixo: o domínio não acessa `Resources`. O ViewModel obtém `" (Cópia)"` de um `StringProvider` (interface em `core.common`, impl com `Context`, falsa nos testes) e o passa em `invoke(sourceId, titleSuffix)`.
- `ScheduleNotFoundException` em `core.schedule.domain.model` (reutilizável por Detail e Snooze).
- **Por que UseCases:** cada um orquestra repositório + alarme com ordem relevante para consistência (regra de negócio de falha parcial), difícil de acertar e testar dentro do ViewModel.

## Estado / eventos / efeitos (acréscimos)
```kotlin
// UiState.Content (novos campos)
val isDeleteDialogVisible: Boolean = false   // restaurado via SavedStateHandle
val isProcessingAction: Boolean = false
val scheduleTitleForDialog / isRecurring: derivados do schedule
// UiEvent
OnDuplicateClicked, OnDeleteClicked, OnConfirmDeleteClicked, OnDismissDeleteDialog, OnOpenCopyClicked(copyId)
// UiEffect
ShowCopyCreated(copyId: String)    // snackbar com ação "Abrir"
NavigateToCopy(copyId: String)     // substitui o detalhe atual
```
- `executeDuplicate()`: ignora se `isProcessingAction`; `onSuccess → ShowCopyCreated(copy.id)`; `onFailure → ShowMessage(mapError(it))`.
- `executeDelete()`: fecha o diálogo, `isProcessingAction = true`; sucesso → `NavigateBack`.
- Como o detalhe observa o item (spec 10), a exclusão fará `observeById` emitir `null`: tratar para não emitir `NavigateBack` duas vezes (usar `hasNavigatedBack` ou deixar só a rota "item removido → voltar" da spec 10 e que `executeDelete` NÃO emita o próprio `NavigateBack`). **Decisão:** `executeDelete` não emite `NavigateBack`; a observação do item faz isso (fonte única); manter teste cobrindo.
- UI: `Snackbar` host no `Scaffold`; `SnackbarResult.ActionPerformed` → `OnOpenCopyClicked`.
- `DeleteConfirmationDialog(title, isRecurring, onConfirm, onDismiss)` → `AlertDialog` com tags; previews: simples, recorrente.
- Ações adicionadas à lista de `ScheduleAction` (spec 10): Duplicar (`ContentCopy`) antes de Editar; Excluir (`Delete`, cor de erro) por último.

## Navegação
`detailEntry(onBack, onEdit, onOpenCopy: (String) -> Unit)`; `AppNavHost`: `onOpenCopy = { id -> backStack.removeLastOrNull(); backStack.add(ScheduleDetail(id)) }`.

## Decisões de UseCase
- `DuplicateScheduleUseCase` e `DeleteScheduleUseCase`: justificados acima (orquestração com semântica de falha parcial).

## Testes
- `DuplicateScheduleUseCaseTest` (MockK): novo id e sufixo; copia todos os campos incl. recorrência e limite; original inexistente → falha sem `upsert`; falha do `upsert` → sem alarme; falha do alarme → sucesso.
- `DeleteScheduleUseCaseTest`: ordem (banco antes do alarme, `coVerifyOrder`); falha do banco → alarme não cancelado; falha do alarme → sucesso.
- `ScheduleDetailViewModelTest` (acréscimos): duplicar emite `ShowCopyCreated`; erro emite mensagem; duplo toque ignora; excluir abre/fecha diálogo; confirmar chama use case e navega (via observação); cancelar não chama.
- `ScheduleDetailScreenRobolectricTest` (acréscimos): botões com `Role.Button` e descrições; diálogo com aviso de recorrência; confirmar/cancelar; Snackbar com "Abrir"; botões desabilitados processando.
