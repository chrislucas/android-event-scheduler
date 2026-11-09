# 19 — Tema claro e escuro persistente — Requisitos

## Introdução
Hoje o app tem **tema escuro fixo** (`core/designsystem/Theme.kt`, "independe do modo do sistema"). Esta spec cria um **tema claro**, deixa o usuário escolher em Configurações entre **Seguir o sistema**, **Claro** e **Escuro**, e **persiste** a escolha: ela sobrevive ao fechamento do app, à morte do processo, à rotação e à atualização do app. A persistência usa o **Preferences DataStore** já existente (arquivo único `remember_me_prefs`). A comparação com `SharedPreferences` está em [`docs/decisoes/persistencia-do-tema-datastore-vs-sharedpreferences.md`](../../../docs/decisoes/persistencia-do-tema-datastore-vs-sharedpreferences.md).

Fora do escopo:
- cores dinâmicas (Material You);
- temas além de claro e escuro (ex.: alto contraste, AMOLED);
- agendar o tema por horário;
- espelhar a escolha no `UiModeManager.setApplicationNightMode` (API 31+), ver "Melhorias futuras" no `design.md`;
- incluir o tema no arquivo exportado pela spec 17.

## Requisitos

### R1 — Escolha do tema
1. Configurações SHALL ter a seção **Aparência** com um grupo de opções exclusivas (`Role.RadioButton`): `Seguir o sistema`, `Claro`, `Escuro`, com a opção atual marcada.
2. WHEN o usuário escolhe uma opção THEN o tema SHALL mudar **imediatamente** em toda a tela visível, sem reiniciar o app e sem depender do botão Salvar de Configurações, e a escolha SHALL ser gravada.
3. A escolha do tema NÃO SHALL descartar nem gravar edições pendentes dos outros campos de Configurações (e-mail, antecedência, adiamento, tela cheia).
4. Valor padrão quando nada foi gravado: **Escuro**, o comportamento atual. Ninguém vê o visual mudar só por atualizar o app.

### R2 — Persistência
1. A escolha SHALL ser persistida no `DataStore<Preferences>` único do app (`remember_me_prefs`), numa chave própria e estável `theme_mode`, com valores estáveis `system`/`light`/`dark` (nunca o `name` do enum).
2. A escolha SHALL sobreviver: ao fechar e reabrir o app, à morte do processo, à rotação e à atualização do app.
3. Valor ilegível ou desconhecido na chave, ou falha de leitura (`IOException`), SHALL resultar no padrão (R1.4), sem crash. O arquivo corrompido já é tratado pelo `ReplaceFileCorruptionHandler` (spec 03).
4. A gravação NÃO SHALL bloquear a thread principal.

### R3 — Aplicação do tema
1. `Seguir o sistema` SHALL usar o modo escuro do sistema e acompanhar a troca do modo do sistema com o app aberto.
2. O tema resolvido SHALL valer para **todas** as telas da `MainActivity` e para a `FullScreenAlarmActivity` (spec 14).
3. Os ícones da barra de status e de navegação SHALL ter contraste com o tema: ícones escuros no claro, claros no escuro (`SystemBarStyle`). O fundo da janela (`window`) SHALL acompanhar o `background` do tema, para não aparecer faixa da outra cor em transições e no teclado.
4. **Sem piscar o tema errado na partida:** a `MainActivity` SHALL manter a SplashScreen do sistema até a preferência ser lida (`setKeepOnScreenCondition`). O conteúdo Compose SHALL ser desenhado já com o tema certo. A SplashScreen continua com o fundo escuro da marca nos dois temas (decisão no `design.md`).
5. A troca de tema SHALL preservar o back stack, a tela aberta e o estado salvo (sem recriar a Activity).

### R4 — Paleta clara e acessibilidade
1. O tema claro SHALL ter um `ColorScheme` próprio em `core/designsystem`, com a mesma identidade (acento ciano), e os pares texto/fundo SHALL atingir contraste **WCAG AA ≥ 4,5:1**: `onBackground`/`background`, `onSurface`/`surface`, `onSurfaceVariant`/`surface` e `surfaceVariant`, `primary`/`background`, `onPrimary`/`primary`, `error`/`surface`. O ciano atual `#26C6DA` sobre branco dá 2,06:1 e **não serve** como cor de texto no claro.
2. O tema escuro atual SHALL continuar idêntico.
3. Cores de conteúdo (cor do agendamento, `EventColorOption`) continuam iguais nos dois temas. Ícone ou texto sobre elas mantém o contraste calculado pela luminância (já feito no `ColorPickerDialog`).
4. Nenhum componente SHALL usar cor fixa de tema (`Color(0x…)`, `Color.White`/`Black`) fora de `core/designsystem`, salvo contraste calculado sobre cor de conteúdo.

### R5 — Previews
1. Telas principais (Home, Configurações, Detalhe, Criar/editar, Pesquisa, Alarme em tela cheia) SHALL ter preview nos dois temas (`@PreviewLightDark` ou duas `@Preview`).
2. As previews existentes que não forem alteradas continuam no tema escuro (o parâmetro `darkTheme` do tema tem padrão `true`).
