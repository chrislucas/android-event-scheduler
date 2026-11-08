# 16 — Autenticação com o bloqueio do dispositivo — Tarefas

- [ ] 1. Dependências: `androidx.biometric:biometric` (estável atual; 1.1.0 em nov/2026 — reavaliar 1.4.0 quando estável), `androidx.lifecycle:lifecycle-process`, `androidx.fragment` (via biometric). `MainActivity` passa a estender `FragmentActivity`.
- [ ] 2. Domain (TDD): `AppLockTimeout`, `AppLockPolicy` + `AppLockPolicyTest`; `AuthResult` e `DeviceCredentialAuthenticator` (R2.2, R2.4, R5.3).
- [ ] 3. `UserSettings` + `UserSettingsRepositoryImpl`: `appLockEnabled`/`appLockTimeout` com padrões e round-trip testado (R6.1).
- [ ] 4. Data (TDD): `DeviceSecurityChecker` e `AppLockController` (ProcessLifecycleOwner, relógio monotônico injetado, estados `Pending`/`Locked`/`Unlocked`, `suppressNextLock`, desativação por dispositivo inseguro) + `AppLockControllerTest` (R2, R3.6, R6.2).
- [ ] 5. Data: `BiometricPromptAuthenticator` (API 30+), `KeyguardIntentAuthenticator` (API 24–29) com `CredentialLauncherRegistry`; `SecurityModule` com o único `if` de versão; mapeamento de erros testado (R5).
- [ ] 6. feature.auth: contrato sealed, `LockViewModel` + testes, `LockScreen`/`LockContent` com previews, strings e tags (R3).
- [ ] 7. app: gate no `AppNavHost` (back stack acima do `if`, deep link só desbloqueado, `BackHandler`), `FLAG_SECURE` na `MainActivity`, registrar o controller no `KoinInitializer` + `AppLockGateRobolectricTest` (R2.1, R3.3, R3.7, R4.1, R4.2).
- [ ] 8. Settings (spec 03): seção "Segurança" com switch autenticado, seletor de tempo, estado de dispositivo inseguro e atalho para as configurações do Android + testes (R1).
- [ ] 9. Integrações: `suppressNextLock()` antes do share sheet (spec 12) e do seletor/compartilhamento de arquivo (spec 17, se já existir); alarme em tela cheia oculta local/notas (spec 14) + testes (R2.3, R4.3).
- [ ] 10. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`; validação manual em emulador API 30+ e API 29 (listar no PR o que foi e não foi executado).

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `AppLockPolicyTest` e `AppLockControllerTest` verdes: timeout (imediato/1 min/5 min, limite exato), relógio monotônico, início bloqueado só se habilitado, supressão de uma volta, dispositivo inseguro desativa e avisa, estado "bloqueado" nunca persistido.
- [ ] Gate (`AppLockGateRobolectricTest`): bloqueado não compõe nenhuma tela da agenda; desbloquear devolve à mesma tela com o back stack intacto; voltar no bloqueio fecha o app; deep link da notificação só é consumido após desbloquear.
- [ ] `FLAG_SECURE` aplicado com a proteção ligada e removido com ela desligada (teste).
- [ ] Configurações: ligar/desligar exige `Success` da autenticação (sem sucesso nada é gravado); switch desabilitado + atalho para as configurações de segurança quando o dispositivo não tem bloqueio seguro (testes).
- [ ] `LockViewModel`/`LockScreen`: autenticação disparada uma vez ao mostrar; cancelar, lockout e indisponível exibem o estado correto com textos de resource (testes).
- [ ] Autenticador por versão: API ≥ 30 usa `BiometricPrompt` com `DEVICE_CREDENTIAL`, API < 30 usa `KeyguardManager` — escolhido só no `SecurityModule`; mapeamento de códigos testado.
- [ ] Alarme em tela cheia com proteção ligada oculta local e notas (teste).
- [ ] PR declara a validação manual do diálogo do sistema (PIN/padrão) em emulador API 30+ e API ≤ 29 — executada ou explicitamente não executada.
