# 09 — Seletor de cor e emoji/adesivo — Requisitos

## Introdução
Dois botões circulares à direita do campo de título (adesivo e cor) na criação/edição de agendamento, e um botão de adesivo ao lado do cabeçalho do dia selecionado na Home ("5 QUI."). A cor aparece nos indicadores do calendário e na lista; o adesivo aparece nos itens e no cabeçalho do dia.

## Requisitos

### R1 — Seletor de adesivo (`StickerPickerDialog`, compartilhado em `core.ui`)
1. SHALL abrir um diálogo (`STICKER_PICKER_DIALOG`) com os 8 emojis de `StickerEmoji` em grade; cada opção com `testTag = STICKER_OPTION_PREFIX + name`, `Role.RadioButton`, `selected` e `contentDescription` = rótulo pt-BR (ex.: "Foguete").
2. SHALL ter botão "Remover adesivo" (`STICKER_CLEAR_BUTTON`) quando houver adesivo selecionado.
3. WHEN o usuário escolhe uma opção THEN o diálogo SHALL fechar devolvendo o emoji (`String`); remover devolve `null`; fechar sem escolher não altera.

### R2 — Seletor de cor (`ColorPickerDialog`, compartilhado em `core.ui`)
1. SHALL abrir um diálogo (`COLOR_PICKER_DIALOG`) com as 6 cores de `EventColorOption`; opção com `COLOR_OPTION_PREFIX + name`, `Role.RadioButton`, `selected`, `contentDescription` = nome da cor ("Azul claro"), e marca de seleção visível (ícone de check, não apenas cor).
2. WHEN escolhida, o diálogo SHALL fechar devolvendo o `hex`.

### R3 — Na criação/edição
1. O campo de título SHALL ser acompanhado de: botão de adesivo (`UPSERT_STICKER_BUTTON`: "Selecionar emoji ou adesivo" / "Adesivo selecionado: 🚀. Toque para alterar") e botão de cor (`UPSERT_COLOR_BUTTON`: "Cor selecionada: Azul claro. Toque para alterar"), ambos com alvo de toque ≥ 48dp.
2. Escolhas SHALL fazer parte do rascunho e serem gravadas em `colorHex`/`stickerEmoji` do `ScheduleItem` ao salvar; na edição SHALL vir preenchidas.
3. Hex fora do catálogo SHALL ser exibido com a cor padrão `LIGHT_BLUE`.

### R4 — Adesivo do dia na Home
1. O cabeçalho do dia (`HOME_DAY_HEADER`) SHALL ter ao lado o botão `HOME_DAY_STICKER_BUTTON` ("Adesivo do dia" / "Adesivo do dia: 🎉. Toque para alterar").
2. Escolher/remover SHALL persistir no DataStore por dia (`epochDay → emoji`) e restaurar ao reabrir o app; cada dia tem seu adesivo.
3. O adesivo do dia SHALL ser exibido também na célula do dia na grade (canto superior), se couber (opcional na UI, obrigatório no dado).

### R5 — Reflexo visual
1. A cor do item SHALL aparecer nos indicadores da grade (spec 05) e no marcador da lista; o adesivo do item SHALL aparecer no `ScheduleListItem` (spec 05).
