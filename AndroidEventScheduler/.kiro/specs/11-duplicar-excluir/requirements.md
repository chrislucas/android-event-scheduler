# 11 — Duplicar e excluir agendamento — Requisitos

## Introdução
Duas ações da barra inferior da tela de detalhes: **Duplicar** cria uma cópia imediata com sufixo "(Cópia)" e seu alarme; **Excluir** remove o agendamento (e o alarme) após confirmação.

## Requisitos

### R1 — Duplicar
1. A barra SHALL ganhar o botão Duplicar (`DETAIL_DUPLICATE_BUTTON`, "Duplicar agendamento"), primeira posição da barra.
2. WHEN tocado THEN SHALL criar um novo agendamento com todos os campos do original, novo `id` e título `"<título> (Cópia)"`, gravar e agendar o alarme (spec 04).
3. A cópia SHALL preservar tipo, datas, horas, cor, adesivo, calendário, alerta, repetição (inclusive limite), local e notas.
4. WHEN concluído THEN a tela SHALL permanecer no original e exibir Snackbar "Cópia criada" com ação "Abrir", que navega para o detalhe da cópia (substituindo a tela atual no back stack).
5. IF o original não existir mais THEN SHALL mostrar erro e nada ser criado.
6. IF a gravação falhar THEN SHALL mostrar mensagem e NÃO agendar alarme; falha só do alarme NÃO SHALL desfazer a cópia.

### R2 — Excluir
1. A barra SHALL ganhar o botão Excluir (`DETAIL_DELETE_BUTTON`, "Excluir agendamento"), última posição.
2. WHEN tocado THEN SHALL abrir diálogo de confirmação (`DELETE_CONFIRM_DIALOG`, "Diálogo de confirmação de exclusão") com título do item e, para recorrentes, aviso de que **todas as ocorrências** serão removidas; botões Excluir (`DELETE_CONFIRM_BUTTON`) e Cancelar (`DELETE_CANCEL_BUTTON`).
3. WHEN confirmado THEN SHALL remover o item do banco e cancelar o alarme pendente; em seguida voltar à tela anterior.
4. WHEN cancelado/dispensado THEN nada SHALL ser alterado.
5. IF a remoção do banco falhar THEN SHALL mostrar mensagem, manter a tela e NÃO cancelar o alarme.
6. IF apenas o cancelamento do alarme falhar THEN o item continua excluído (o receiver ignora alarmes de itens inexistentes).
7. O estado do diálogo SHALL sobreviver à rotação.

### R3 — Proteção contra toques repetidos
1. Enquanto uma ação roda (`isProcessingAction`), os botões da barra SHALL ficar desabilitados e nenhuma ação duplicada SHALL ser disparada.
