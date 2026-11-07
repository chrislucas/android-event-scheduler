# 16 — Autenticação com o bloqueio do dispositivo — Requisitos

## Introdução
Proteção opcional do app com a **credencial de desbloqueio já configurada no celular** (PIN, padrão ou senha). Quando ativada, o conteúdo da agenda só aparece depois que o usuário confirmar a credencial na tela do sistema. O app **não cria nem guarda** PIN/padrão próprios: delega ao sistema operacional. Biometria fica fora do escopo (ver R7).

## Requisitos

### R1 — Ativar e desativar (Configurações)
1. A tela de Configurações (spec 03) SHALL ganhar a seção "Segurança" com o *switch* "Bloquear o app com o bloqueio de tela" (`SETTINGS_APP_LOCK_SWITCH`, `Role.Switch`), **desligado por padrão**.
2. WHEN o usuário liga o switch THEN o app SHALL pedir a credencial do dispositivo **antes** de salvar a preferência; só ativa se a confirmação for bem-sucedida. Cancelar ou falhar mantém desligado.
3. WHEN o usuário desliga o switch THEN o app SHALL também pedir a credencial (impede que alguém com o celular desbloqueado desative a proteção sem saber o PIN).
4. IF o dispositivo **não** tiver bloqueio de tela seguro (`KeyguardManager.isDeviceSecure == false`) THEN o switch SHALL ficar desabilitado com o texto de apoio "Configure um PIN, padrão ou senha no Android para usar este recurso" e um botão "Abrir configurações de segurança" (`Settings.ACTION_SECURITY_SETTINGS`).
5. Com a proteção ligada, a seção SHALL exibir o seletor "Pedir novamente após" com as opções **Imediatamente**, **1 minuto** (padrão) e **5 minutos** (`SETTINGS_APP_LOCK_TIMEOUT_OPTION_PREFIX`).

### R2 — Quando o app fica bloqueado
1. WHEN a proteção está ligada e o app é aberto do zero (processo novo) THEN o app SHALL iniciar **bloqueado**.
2. WHEN o app vai para segundo plano e volta THEN SHALL bloquear se o tempo em segundo plano for **maior ou igual** ao configurado em R1.5 ("Imediatamente" = qualquer ida ao segundo plano).
3. Rotação, troca de tema/idioma e abrir um diálogo do próprio app (ex.: share sheet, seletor de arquivo da spec 17) SHALL NOT bloquear.
4. O instante de ida ao segundo plano SHALL usar relógio **monotônico** (`SystemClock.elapsedRealtime`), para que mudar a hora do sistema não burle o tempo limite.
5. WHEN a proteção é desligada THEN o app SHALL desbloquear imediatamente e não bloquear mais.

### R3 — Tela de bloqueio
1. Enquanto bloqueado, o app SHALL mostrar a tela "RememberMe bloqueado" (`LOCK_SCREEN`) **no lugar** de qualquer conteúdo (nenhum dado da agenda é composto), com ícone de cadeado, texto "Confirme o bloqueio de tela do celular para continuar" e o botão "Desbloquear" (`LOCK_UNLOCK_BUTTON`).
2. WHEN a tela de bloqueio aparece THEN SHALL abrir automaticamente a confirmação do sistema uma vez; o botão "Desbloquear" a reabre.
3. WHEN a confirmação é bem-sucedida THEN o app SHALL desbloquear e exibir exatamente a tela em que o usuário estava (back stack preservado).
4. WHEN o usuário cancela THEN SHALL permanecer na tela de bloqueio, sem erro.
5. WHEN o sistema bloqueia temporariamente por tentativas demais (`ERROR_LOCKOUT`/`ERROR_LOCKOUT_PERMANENT`) THEN SHALL mostrar "Muitas tentativas. Tente novamente mais tarde." e manter o botão disponível.
6. IF o bloqueio de tela do celular for **removido** enquanto a proteção está ligada THEN, ao abrir, o app SHALL desligar a proteção automaticamente e mostrar uma vez "O bloqueio de tela do celular foi removido; a proteção do app foi desativada" (não há como autenticar; evitar trancar o usuário fora dos próprios dados).
7. O botão voltar do sistema na tela de bloqueio SHALL fechar o app (não revela conteúdo).

### R4 — Privacidade
1. Com a proteção ligada, a `MainActivity` SHALL usar `FLAG_SECURE` para que a miniatura em "Recentes" não mostre a agenda e capturas de tela fiquem bloqueadas; com a proteção desligada, a flag SHALL ser removida.
2. Notificações de lembrete (spec 04) continuam mostrando título e horário — são o propósito do app. Com a proteção ligada, o toque na notificação SHALL abrir o app **bloqueado** e, após desbloquear, ir ao detalhe (deep link preservado).
3. A tela de alarme em tela cheia (spec 14) SHALL continuar funcionando sem autenticação (é um alerta, aparece sobre a tela de bloqueio do próprio sistema), mas, com a proteção ligada, SHALL ocultar **local e notas** do evento, mostrando só título e horário. "Adiar" e "Dispensar" não exigem autenticação.

### R5 — Plataforma
1. Android 11+ (API 30+): SHALL usar `BiometricPrompt` com `setAllowedAuthenticators(DEVICE_CREDENTIAL)`.
2. Android 7–10 (API 24–29): `DEVICE_CREDENTIAL` isolado não é suportado pelo `BiometricPrompt`; SHALL usar `KeyguardManager.createConfirmDeviceCredentialIntent` via Activity Result.
3. A escolha da implementação SHALL ficar atrás de uma interface (`DeviceCredentialAuthenticator`), sem `if (SDK_INT)` espalhado pelo código.

### R6 — Persistência
1. As preferências `appLockEnabled` e `appLockTimeout` SHALL ser persistidas no DataStore único (via `UserSettings`, spec 03).
2. O estado "bloqueado/desbloqueado" SHALL ficar **só em memória** (nunca persistido): matar o processo sempre volta bloqueado.

### R7 — Fora do escopo
1. Biometria (digital/rosto) como alternativa ao PIN — pode entrar depois com `BIOMETRIC_STRONG | DEVICE_CREDENTIAL` (API 30+).
2. Criptografia do banco de dados vinculada à autenticação (chave no Keystore). Esta spec é **controle de acesso à UI**, não criptografia em repouso; isso deve ficar explícito na documentação e na tela de Configurações ("protege a abertura do app").
