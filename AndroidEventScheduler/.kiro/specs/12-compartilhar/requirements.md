# 12 — Compartilhar agendamento — Requisitos

## Introdução
O botão "Compart..." da tela de detalhes abre o *share sheet* nativo do Android com um resumo em texto do agendamento.

## Requisitos

### R1 — Ação
1. A barra de ações SHALL ganhar o botão Compartilhar (`DETAIL_SHARE_BUTTON`, rótulo "Compartilhar", `contentDescription` "Compartilhar agendamento"), na terceira posição (Duplicar, Editar, **Compartilhar**, Excluir). Em telas estreitas o rótulo visível PODE ser truncado ("Compart…"), sem afetar a descrição de acessibilidade.
2. WHEN tocado THEN SHALL abrir o seletor do sistema (`Intent.ACTION_SEND`, `text/plain` + `createChooser`) com assunto `RememberMe: <título>` e corpo com o resumo.

### R2 — Conteúdo do resumo
1. O corpo SHALL conter, uma informação por linha, nesta ordem: `📅 <título>`, `🕒 <intervalo de data/hora>` (mesmo texto da tela de detalhes), `🔔 Alerta: <alerta>`, `🔁 Repetição: <repetição>`, `📍 Local: <local>` (somente se houver), `📝 Notas: <notas>` (somente se houver).
2. Textos SHALL vir de recursos pt-BR e SHALL ser os mesmos formatadores da tela de detalhes (sem divergência de formato).
3. O resumo SHALL NOT incluir o `id` interno nem o e-mail da conta.

### R3 — Robustez
1. IF nenhum app puder tratar o envio (`ActivityNotFoundException`) THEN SHALL mostrar mensagem "Não foi possível compartilhar" sem crash.
2. IF o item ainda não carregou THEN o botão SHALL estar desabilitado.
3. O compartilhamento SHALL ser lançado a partir da `Activity` atual (sem `FLAG_ACTIVITY_NEW_TASK`) e SHALL NOT vazar a Activity.
4. A ação NÃO SHALL alterar dados nem exigir permissões.
