# 18 — Toque duplo numa data cria evento ou lembrete — Requisitos

## Introdução
Atalho na grade mensal da Home (spec 05): **tocar duas vezes num dia** abre a criação de agendamento (spec 07) com esse dia como data inicial. Na tela de criação, o usuário escolhe **Evento** ou **Lembrete** pelas abas que já existem. O toque simples continua só selecionando o dia, **sem atraso**. A barra "Adicionar agendamento" (R5 da spec 05) continua igual.

Fora do escopo: pressionar e segurar, arrastar para criar intervalo, criação inline sem abrir a tela de criação e escolha do tipo antes de abrir a tela (ver "Decisões" no `design.md`).

## Requisitos

### R1 — Toque duplo abre a criação
1. WHEN o usuário toca duas vezes no **mesmo** dia da grade, dentro do intervalo de toque duplo do sistema (`ViewConfiguration.doubleTapTimeoutMillis`), THEN a Home SHALL abrir a criação de agendamento com esse dia como data inicial (`UpsertSchedule(initialDateEpochDay = <dia>)`).
2. A tela de criação SHALL abrir no estado de **novo** agendamento, com as abas Evento/Lembrete e o padrão da spec 07 (Lembrete selecionado, próxima hora cheia). O usuário SHALL poder trocar para Evento antes de salvar.
3. WHEN o usuário salva THEN o agendamento SHALL ser gravado no dia tocado e a Home SHALL mostrá-lo na agenda desse dia (atualização reativa da spec 05, R6.1).
4. WHEN o usuário cancela ou volta THEN a Home SHALL voltar com o dia tocado selecionado e nada gravado.
5. Datas passadas SHALL ser aceitas, como na adição rápida.

### R2 — Toque simples não muda
1. O **primeiro** toque SHALL selecionar o dia **imediatamente**, sem esperar pelo intervalo de toque duplo (a seleção não pode atrasar).
2. Dois toques em dias **diferentes**, mesmo rápidos, SHALL apenas selecionar cada dia, sem abrir a criação.
3. Dois toques no mesmo dia **fora** do intervalo SHALL ser dois toques simples, sem abrir a criação.
4. Três toques rápidos no mesmo dia SHALL abrir a criação **uma única vez**. Depois de um toque duplo, a contagem recomeça.
5. Deslizar na grade para trocar de mês (spec 05, R1.5) SHALL NOT contar como toque, mesmo que o dedo termine dentro de uma célula. Um toque só conta se o deslocamento entre tocar e soltar for menor que o `touchSlop` do sistema.

### R3 — Dias de meses vizinhos
1. WHEN o toque duplo for num dia esmaecido de mês vizinho THEN o mês visível SHALL mudar para o mês desse dia (spec 05, R1.4) **e** a criação SHALL abrir com esse dia.

### R4 — Acessibilidade
1. Com TalkBack, o toque duplo é o gesto de **ativar** e não chega à grade como dois toques. Cada célula SHALL oferecer uma **ação de acessibilidade personalizada** com o rótulo `Adicionar agendamento em 5 de novembro` (resource formatado), que abre a criação para o dia, como no R1.
2. A ativação normal (TalkBack, teclado, Switch Access) SHALL continuar selecionando o dia (spec 05, R8).
3. O `contentDescription` da célula (spec 05, R8) SHALL continuar igual.

### R5 — Descoberta do atalho
1. O estado vazio do dia (`HOME_EMPTY_DAY`) SHALL ganhar uma segunda linha `Toque duas vezes no dia para adicionar` (resource), com tag própria `HOME_EMPTY_DAY_HINT`.

### R6 — Robustez
1. Se o toque duplo ocorrer com a criação já em processo de abertura (efeito pendente), SHALL abrir **uma** tela de criação, não duas.
2. O detector de toque duplo SHALL ser puro e ter o tempo injetado (sem `System.currentTimeMillis` / `SystemClock` dentro da regra), para testes determinísticos.
