# 12 — Compartilhar agendamento — Tarefas

- [ ] 1. TDD: `ScheduleShareTextBuilder` + `ShareContent` + strings pt-BR + `ScheduleShareTextBuilderTest` (R2).
- [ ] 2. `OnShareClicked` / `ShareText` no contrato de presentation e no `ScheduleDetailViewModel` + testes (R1.2, R3.2).
- [ ] 3. `ShareLauncher` + `ShareLauncherRobolectricTest` (R1.2, R3.1, R3.3).
- [ ] 4. Botão Compartilhar na `ScheduleActionBar` (posição 3), coleta do efeito na tela, mensagem de erro + teste de tela (R1.1, R3).
- [ ] 5. Registrar `ScheduleShareTextBuilder` no `DetailModule`; atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `ScheduleShareTextBuilderTest`: texto completo, sem local, sem notas, recorrente, assunto; sem `id` nem e-mail.
- [ ] `ShareLauncherRobolectricTest`: chooser com `ACTION_SEND`, `text/plain`, assunto e texto; sem handler → `false` e mensagem, sem crash.
- [ ] Botão Compartilhar na 3ª posição da barra, desabilitado enquanto carrega; nenhum `ShareManager`/UseCase criado.
