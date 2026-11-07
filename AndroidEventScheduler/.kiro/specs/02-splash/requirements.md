# 02 — Splash — Requisitos

## Introdução
Tela inicial com a marca "RememberMe" em fundo escuro e ícone circular ciano. Depois de um breve intervalo navega para a Home, removendo a Splash do back stack.

## Requisitos

### R1 — Conteúdo
1. A tela SHALL exibir um ícone circular ciano (`#26C6DA`) e o texto "RememberMe" sobre fundo `#0A0A0A`.
2. O logo SHALL ter `testTag = SPLASH_LOGO`, `role = Image` e `contentDescription = "Logo do aplicativo RememberMe"`.
3. O título SHALL ter `testTag = SPLASH_TITLE` e `heading()`.

### R2 — Navegação
1. WHEN a tela é exibida (`OnSplashStarted`) THEN o ViewModel SHALL aguardar `SPLASH_DELAY_MILLIS` (1500 ms) e emitir `NavigateToHome`.
2. WHEN `NavigateToHome` é consumido THEN o app SHALL substituir a Splash pela Home (voltar na Home sai do app, não volta à Splash).
3. WHEN a tela é recriada (rotação) após a navegação THEN o efeito SHALL NOT ser reemitido.
4. O atraso SHALL ser cancelado se a tela sair da composição antes do fim.

### R3 — Sistema
1. O app SHALL usar a `SplashScreen` API do sistema (`androidx.core:core-splashscreen`) com tema `Theme.AndroidEventScheduler.Starting` escuro, para evitar flash branco; a tela Compose continua sendo a Splash "de marca".
