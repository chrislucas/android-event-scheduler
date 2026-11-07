# 09 — Seletor de cor e emoji/adesivo — Design

## Pacotes
```
core/ui/component/    StickerPickerDialog.kt, ColorPickerDialog.kt, StickerButton.kt, ColorDotButton.kt   # compartilhados (Upsert + Home)
feature/upsert/presentation/component/TitleRow.kt        (alterado: ganha os 2 botões)
feature/home/
├── domain/repository/HomePreferencesRepository.kt       (+ observeDaySticker / setDaySticker)
├── data/repository/HomePreferencesRepositoryImpl.kt     (+ chaves day_sticker_<epochDay>)
└── presentation/ … DayHeader.kt (novo componente), HomeViewModel/State/Event (alterados)
```
Os catálogos `StickerEmoji`/`EventColorOption` já existem em `core.schedule.domain.model` (spec 01); os diálogos ficam em `core.ui` porque duas features os usam.

## Componentes compartilhados
```kotlin
@Composable fun StickerPickerDialog(selectedEmoji: String?, onStickerSelected: (String?) -> Unit, onDismiss: () -> Unit)
@Composable fun ColorPickerDialog(selectedColorHex: Long, onColorSelected: (Long) -> Unit, onDismiss: () -> Unit)
@Composable fun StickerButton(emoji: String?, description…, onClick: () -> Unit, testTag: String)   // 48dp; ícone Face quando null
@Composable fun ColorDotButton(colorHex: Long, onClick: () -> Unit, testTag: String)                // 48dp de alvo, círculo 28dp
```
- Grade com `LazyVerticalGrid`/`FlowRow` dentro de `Dialog`; cada opção `Modifier.selectable(role = RadioButton)`.
- `contentDescription` do `StickerButton`/`ColorDotButton` é fornecido por quem usa (strings pt-BR com placeholder).
- Previews: sem seleção; com seleção; cada diálogo.

## Upsert
- `UpsertScheduleUiState.Content`: já tem `colorHex`/`stickerEmoji` (spec 07) + `isStickerPickerVisible`, `isColorPickerVisible`.
- Eventos: `OnStickerButtonClicked`, `OnStickerPickerDismissed`, `OnStickerSelected(String?)`, `OnColorButtonClicked`, `OnColorPickerDismissed`, `OnColorSelected(Long)`.
- `buildItem` (spec 07) já copia `colorHex`/`stickerEmoji`; Lembrete também pode ter cor/adesivo.

## Home — adesivo do dia
```kotlin
// HomePreferencesRepository (acréscimo)
fun observeDaySticker(epochDay: Long): Flow<String?>
suspend fun setDaySticker(epochDay: Long, emoji: String?)     // null/blank remove a chave
```
- Chave `stringPreferencesKey("day_sticker_$epochDay")`. Cada dia = uma chave (simples; volume baixo).
- `HomeViewModel`: `selectedDateFlow.flatMapLatest { repo.observeDaySticker(it.toEpochDay()) }` entra no `combine` → campo `selectedDaySticker: String?` em `HomeUiState.Content`; eventos `OnDayStickerClicked`, `OnDayStickerPickerDismissed`, `OnDayStickerSelected(String?)`; estado `isDayStickerPickerVisible`.
- `DayHeader(date, sticker, onStickerClick)` compõe `5 QUI.` + `StickerButton` (`HOME_DAY_STICKER_BUTTON`).
- Opcional: `CalendarDayCell.sticker` — exigiria observar vários dias; **adiar**: se implementado, usar `observeDayStickers(range): Flow<Map<Long,String>>` em vez de N flows. Fora do escopo mínimo (R4.3 opcional na UI).

## Decisões de UseCase
Nenhum: seleção é estado de apresentação e persistência é passa-direto.

## Testes
- `StickerPickerDialogRobolectricTest`: 8 opções com rótulo; seleção devolve emoji; limpar devolve `null`; dismiss não chama callback de seleção; `Role.RadioButton`/`selected`.
- `ColorPickerDialogRobolectricTest`: 6 opções; check visível só na selecionada; devolve hex.
- `UpsertScheduleViewModelTest` (acréscimo): escolher cor/adesivo atualiza rascunho e persiste ao salvar (use case chamado com os valores); edição vem preenchida; hex desconhecido → botão mostra cor padrão.
- `HomePreferencesRepositoryImplTest` (acréscimo): set/observe/remove adesivo do dia; dias independentes.
- `HomeViewModelTest` (acréscimo): trocar o dia observa o adesivo do novo dia; selecionar persiste.
- `UpsertScheduleScreenRobolectricTest`/`HomeScreenRobolectricTest` (acréscimos): semântica dos dois botões e fluxo abrir→escolher→descrição atualizada.
