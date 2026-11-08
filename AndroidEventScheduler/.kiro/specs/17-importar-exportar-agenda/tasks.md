# 17 — Importar e exportar a agenda — Tarefas

- [x] 1. `core.schedule`: `ScheduleRepository.upsertAll(items)` com `@Transaction` no DAO + teste de atomicidade; rota `AgendaBackup(pendingImportUri)` em `core.navigation` (R6.1).
- [ ] 2. Domain: modelos (`ImportPreview`, `ImportStrategy`, `ImportResult`, `ExportTarget`, `ExportResult`, `BackupError`) e interface `AgendaBackupRepository`.
- [ ] 3. Data (TDD): DTOs `@Serializable`, `AgendaJsonCodec`, `AgendaDtoMapper` (validação por item, truncamento) + testes; `docs/formato-agenda-json.md` com exemplo (R2, R5.1–R5.3, R7.3).
- [ ] 4. Domain (TDD): `ImportAgendaUseCase` + `ImportAgendaUseCaseTest` (R5.5, R6).
- [ ] 5. Data (sem nenhuma permissão nova — confirmar no `RequestedPermissionsGuardRobolectricTest` e registrar em `docs/permissoes.md` que o backup não usa armazenamento): `ExportFileStore`, leitura com limite de 5 MB, `AgendaBackupRepositoryImpl` + testes; `FileProvider` e `file_paths.xml` no manifest (R3.5, R4.3, R7.1, R7.2).
- [ ] 6. Presentation: contrato sealed, `BackupViewModel` + testes (R3, R5, R6.3).
- [ ] 7. Presentation: `BackupScreen` e componentes com previews, launchers SAF, `ShareFile` com `ClipData` e tratamento de `ActivityNotFoundException`, strings/`plurals`/tags + testes de tela e de intent (R1.2, R3.1–R3.6, R4.1, R8).
- [ ] 8. Entrada: item "Importar e exportar agenda" em Configurações (spec 03) e `backupEntry` no `AppNavHost` (R1.1).
- [ ] 9. Receber arquivos: `intent-filter` `VIEW`/`SEND` `application/json`, `IncomingFileState`, abertura da pré-visualização respeitando o bloqueio da spec 16 (se presente) + teste (R4.2, R7.5).
- [ ] 10. `BackupModule` em `AppModules`; atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`; validação manual em emulador (compartilhar por e-mail, salvar, abrir anexo) — listar no PR o que foi e não foi executado.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `AgendaJsonCodecTest` e `AgendaDtoMapperTest`: round-trip sem perda de nenhum campo/padrão de recorrência; `format` errado, `version` futura e JSON corrompido rejeitados; itens inválidos ignorados e contados; strings truncadas.
- [ ] `ImportAgendaUseCaseTest`: as três estratégias; gravação em uma transação; alarme agendado por item; falha de alarme não desfaz; falha da gravação não agenda nada; reimportar com "Substituir" é idempotente.
- [ ] `upsertAll` atômico comprovado em teste (falha no meio não grava nada).
- [ ] Exportação: share sheet com `ACTION_SEND`, `application/json`, `EXTRA_STREAM` (URI do `FileProvider` restrito a `cache/exports/`), assunto, texto e permissão de leitura; "Salvar" grava via SAF; cache limpo a cada exportação; agenda vazia desabilita (testes).
- [ ] Importação: limite de 5 MB aplicado mesmo sem tamanho informado; pré-visualização mostra novos/existentes/inválidos; arquivo inválido mostra o motivo e não grava nada (testes).
- [ ] Arquivo recebido por `ACTION_VIEW`/`ACTION_SEND` abre a pré-visualização (teste); nenhuma permissão de armazenamento no manifest.
- [ ] `docs/formato-agenda-json.md` publicado com exemplo válido (o exemplo é lido com sucesso por um teste).
- [ ] PR declara a validação manual (enviar por e-mail, salvar no seletor, abrir anexo com o RememberMe) — executada ou explicitamente não executada.
