# 17 — Importar e exportar a agenda — Requisitos

## Introdução
Backup e transferência da agenda em arquivo. O usuário **exporta** todos os agendamentos para um arquivo `.json` e escolhe: **compartilhar** (e-mail, Drive, WhatsApp, Bluetooth ou qualquer app compatível instalado, pelo share sheet do sistema) ou **salvar** num local escolhido (Storage Access Framework). Também **importa** um arquivo exportado pelo RememberMe — escolhido no seletor de arquivos ou recebido de outro app ("Abrir com" / "Compartilhar com" RememberMe) — com pré-visualização e escolha do que fazer com itens repetidos. Tudo offline; nada é enviado pelo app a servidores.

## Requisitos

### R1 — Acesso
1. A tela de Configurações (spec 03) SHALL ganhar o item "Importar e exportar agenda" (`SETTINGS_BACKUP_ITEM`) que abre a tela de backup (rota `AgendaBackup`).
2. A tela de backup SHALL ter duas seções: **Exportar** e **Importar**, cada uma com uma frase explicando o que acontece.

### R2 — Formato do arquivo
1. O arquivo SHALL ser JSON UTF-8 com cabeçalho `{"format": "rememberme-agenda", "version": 1, "exportedAt": "<ISO-8601>", "appVersion": "<versionName>", "items": [...]}`.
2. Cada item SHALL conter **todos** os campos do `ScheduleItem` (spec 01): `id`, `title`, `type`, `startDate`, `endDate` (`AAAA-MM-DD`), `startTime`, `endTime` (`HH:mm`), `isAllDay`, `location`, `notes`, `colorHex`, `calendarSource`, `reminderOffset`, `recurrence` (`{type, daysOfWeek, occurrenceLimit}`), `stickerEmoji`. Enums por **nome estável**, não por ordinal.
3. O nome do arquivo SHALL ser `rememberme-agenda-AAAA-MM-DD.json` (data local da exportação).
4. Configurações do usuário (e-mail da conta, preferências), filtros da gaveta e adesivos do dia SHALL NOT ser exportados (dados pessoais/de dispositivo, fora do escopo da agenda).
5. O formato SHALL ser documentado em `docs/formato-agenda-json.md` com um exemplo, para permitir evolução (`version`).

### R3 — Exportar
1. "Compartilhar arquivo" (`BACKUP_EXPORT_SHARE_BUTTON`) SHALL gerar o arquivo e abrir o share sheet (`ACTION_SEND`, MIME `application/json`, `EXTRA_STREAM` com URI do `FileProvider`, `FLAG_GRANT_READ_URI_PERMISSION`), com `EXTRA_SUBJECT` "Agenda RememberMe — <data>" e `EXTRA_TEXT` curto ("Backup da agenda com N agendamentos."), de modo que apps de **e-mail** já recebam assunto, corpo e anexo.
2. "Salvar em arquivo" (`BACKUP_EXPORT_SAVE_BUTTON`) SHALL abrir o seletor do sistema (`ACTION_CREATE_DOCUMENT`) com o nome sugerido em R2.3 e gravar no local escolhido.
3. IF a agenda estiver vazia THEN os dois botões SHALL ficar desabilitados com o texto "Não há agendamentos para exportar".
4. Durante a geração SHALL mostrar progresso e impedir toques repetidos; ao terminar "Salvar", mostrar "Agenda exportada (N agendamentos)".
5. Arquivos temporários de compartilhamento SHALL ficar em `cacheDir/exports/` e os anteriores SHALL ser apagados a cada nova exportação (sem acúmulo).
6. IF nenhum app puder receber o compartilhamento THEN SHALL mostrar "Nenhum app disponível para compartilhar" sem crash.

### R4 — Importar: origem do arquivo
1. "Escolher arquivo" (`BACKUP_IMPORT_PICK_BUTTON`) SHALL abrir o seletor do sistema (`ACTION_OPEN_DOCUMENT`, MIME `application/json` e `*/*` como alternativa, já que alguns provedores marcam `.json` como `application/octet-stream`).
2. O app SHALL aceitar arquivos recebidos de outros apps: `ACTION_VIEW` e `ACTION_SEND` com MIME `application/json` (ex.: anexo de e-mail aberto "com RememberMe"). Ao receber, SHALL abrir a tela de backup já na pré-visualização da importação (respeitando a autenticação da spec 16, se ativa).
3. Arquivos acima de **5 MB** SHALL ser recusados antes da leitura completa ("Arquivo grande demais para uma agenda do RememberMe").

### R5 — Importar: validação e pré-visualização
1. O arquivo SHALL ser validado **por completo antes de gravar qualquer coisa**: JSON válido, `format == "rememberme-agenda"`, `version` suportada (≤ 1), cada item com campos obrigatórios e valores válidos (datas, `endDate ≥ startDate`, `Weekly` com ≥ 1 dia, limite ≥ 1, enums conhecidos).
2. IF o arquivo for inválido THEN SHALL mostrar o motivo em linguagem simples ("Este arquivo não é um backup do RememberMe", "Versão do arquivo mais nova que o app — atualize o RememberMe", "Arquivo corrompido") e **nada** é importado.
3. Itens individualmente inválidos num arquivo válido SHALL ser **ignorados** e contados (ex.: "2 itens ignorados por dados inválidos"); os demais podem ser importados.
4. A pré-visualização (`BACKUP_IMPORT_PREVIEW`) SHALL mostrar: total no arquivo, quantos são **novos** (id inexistente), quantos **já existem** (mesmo id) e quantos serão ignorados por invalidez.
5. A pré-visualização SHALL oferecer a escolha para itens que já existem (`selectableGroup`, `Role.RadioButton`):
   - **Substituir pelos do arquivo** (padrão);
   - **Manter os atuais** (importa só os novos);
   - **Importar como cópias** (novos ids, título com " (Importado)").
6. Botões "Importar" (`BACKUP_IMPORT_CONFIRM_BUTTON`) e "Cancelar".

### R6 — Importar: gravação
1. A gravação SHALL ser **atômica** (uma transação Room): ou todos os itens selecionados entram, ou nenhum.
2. Após gravar, SHALL agendar o próximo alarme de cada item importado/substituído via `ScheduleNextAlarmUseCase` (spec 04); itens passados ou sem alerta ficam sem alarme. Falha ao agendar um alarme NÃO desfaz a importação.
3. Ao terminar SHALL mostrar "N agendamentos importados" e voltar; a Home reflete os itens imediatamente (observação reativa da spec 05).
4. Importar o mesmo arquivo duas vezes com "Substituir" SHALL ser idempotente (mesmo resultado).

### R7 — Privacidade e segurança
1. O app SHALL NOT pedir permissão de armazenamento (`READ/WRITE_EXTERNAL_STORAGE`): só SAF e `FileProvider`.
2. O `FileProvider` SHALL expor **apenas** `cacheDir/exports/` (`file_paths.xml`), `exported=false`, com permissão de leitura concedida por URI.
3. O conteúdo do arquivo SHALL ser tratado como não confiável: parser com limites (R4.3), sem execução de nada, strings truncadas ao tamanho máximo dos campos (título 200, local 200, notas 4000).
4. A tela de backup SHALL avisar: "O arquivo não é criptografado; quem tiver o arquivo poderá ver sua agenda."
5. Com a autenticação da spec 16 ativa, abrir a tela de backup ou receber um arquivo SHALL exigir o desbloqueio como qualquer outra tela; abrir o share sheet/seletor SHALL chamar `suppressNextLock()`.

### R8 — Acessibilidade e textos
1. Todos os textos de `strings.xml` (contagens com `plurals`), botões com `Role.Button`, progresso anunciado (`liveRegion`), tags `BACKUP_*` em `SemanticsTags`.
