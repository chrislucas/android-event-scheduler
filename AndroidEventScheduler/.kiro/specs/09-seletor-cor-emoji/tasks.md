# 09 — Seletor de cor e emoji/adesivo — Tarefas

- [x] 1. `core.ui.format`: rótulos de acessibilidade em `strings.xml` para `StickerEmoji` ("Rosto sorridente", "Livros de estudo", "Computador e programação", "Foguete", "Estrela", "Comemoração", "Treino", "Pausa para café") e `EventColorOption` ("Azul claro", "Índigo", "Verde-azulado", "Verde oliva", "Roxo", "Coral") com `StickerEmoji.labelRes()`/`EventColorOption.labelRes()` + teste (o domínio da spec 01 não carrega texto). Depois, `core.ui.component`: `StickerPickerDialog`, `ColorPickerDialog`, `StickerButton`, `ColorDotButton` + strings/tags/previews + testes Robolectric (R1, R2).
- [x] 2. Upsert: eventos/estado dos pickers e `TitleRow` com os dois botões (R3) + testes de ViewModel (TDD).
- [x] 3. (exige a spec 06 concluída — `HomePreferencesRepository`) Home dados: `observeDaySticker`/`setDaySticker` no repositório e implementação + testes (R4.2).
- [x] 4. Home apresentação: estado/eventos/ViewModel do adesivo do dia, `DayHeader`, diálogo (R4.1) + testes.
- [x] 5. Conferir reflexo visual de cor e adesivo nos indicadores e na lista (R5) com teste de tela.
- [x] 6. Atualizar `KoinModulesVerificationTest` se houver mudança de módulos; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `StickerPickerDialog` e `ColorPickerDialog` testados: 8/6 opções, `Role.RadioButton`, seleção, limpar, dismiss; cor com marca de check.
- [x] Upsert: cor e adesivo entram no rascunho, são salvos e vêm preenchidos na edição.
- [x] Home: adesivo do dia persiste por `epochDay`, independente entre dias, restaurado ao reabrir (`HomePreferencesRepositoryImplTest`).
- [x] Cor e adesivo do item aparecem nos indicadores e na lista (teste de tela).
