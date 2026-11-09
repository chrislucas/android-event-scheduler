# 19 — Tema claro e escuro persistente — Tarefas

- [ ] 1. TDD: `ThemeMode` (+ `fromStorageId`) e `isDark(systemDark)` + `ThemeModeTest`, `ThemeResolutionTest` (R1.4, R2.1, R3.1).
- [ ] 2. TDD: `ThemePreferenceRepository` + `ThemePreferenceRepositoryImpl` no DataStore único + registro no `PreferencesModule` + `ThemePreferenceRepositoryImplTest`. Interface, implementação, fakes de teste e Koin no **mesmo commit** (R2).
- [ ] 3. Paleta clara em `Color.kt`, `RememberMeLightColorScheme`, `AndroidEventSchedulerTheme(darkTheme = true, …)` + `ColorContrastTest` (R4.1, R4.2, R5.2).
- [ ] 4. `SystemThemeEffects` (barras do sistema e fundo da janela) com preview do conteúdo de exemplo (R3.3).
- [ ] 5. `MainActivity`: `StateFlow<ThemeMode?>`, `setKeepOnScreenCondition`, tema resolvido + `SystemThemeEffects`; `FullScreenAlarmActivity` com o mesmo tema (R3.2, R3.4, R3.5).
- [ ] 6. Configurações: família `AppearanceEvent`, `themeMode` no `Content` (fora do `draft`), seção Aparência com três opções de rádio, strings e tags + acréscimos no `SettingsViewModelTest` e `SettingsScreenRobolectricTest` (R1).
- [ ] 7. Varredura de cores fixas fora de `core/designsystem` (R4.4) e previews claro/escuro nas telas principais (R5.1).
- [ ] 8. Ponta a ponta `ThemeRobolectricTest` e acréscimo no `FullScreenAlarmActivityRobolectricTest` (R2.2, R3).
- [ ] 9. Verificação final conforme [`criterios-de-aceite.md`](../criterios-de-aceite.md), incluindo a checagem por commit.

## Critérios de aceite

A spec só é considerada concluída, e o PR só é aberto, quando **todos** estiverem marcados `[x]`, cada um verificado de fato e não presumido. Somam-se a eles os **critérios gerais** em [`.kiro/specs/criterios-de-aceite.md`](../criterios-de-aceite.md).

- [ ] Persistência no **Preferences DataStore** único (`remember_me_prefs`), chave `theme_mode` com ids estáveis. Nenhum `SharedPreferences`, Proto DataStore ou dependência nova.
- [ ] `ThemePreferenceRepositoryImplTest` prova a persistência com uma **nova instância** lendo o mesmo arquivo, além de padrão, valor desconhecido e `IOException` → Escuro.
- [ ] Trocar o tema em Configurações aplica na hora, **sem recriar a Activity**, e não grava nem descarta edições pendentes do formulário.
- [ ] Padrão sem preferência = **Escuro**; o tema escuro ficou idêntico ao anterior (os testes e previews existentes passam sem edição).
- [ ] `Seguir o sistema` acompanha o modo do sistema (`night` × `notnight` no Robolectric).
- [ ] Barras do sistema com ícones no contraste certo nos dois temas (`isAppearanceLightStatusBars`) e fundo da janela igual ao `background` do tema.
- [ ] A SplashScreen fica até a preferência ser lida (`setKeepOnScreenCondition`); o primeiro quadro do Compose já sai no tema certo.
- [ ] `FullScreenAlarmActivity` respeita o tema escolhido.
- [ ] `ColorContrastTest`: todos os pares do R4.1 ≥ 4,5:1 nas duas paletas.
- [ ] Nenhuma cor fixa de tema fora de `core/designsystem` (varredura registrada no PR).
- [ ] Telas principais com preview claro e escuro.
- [ ] Não verificado em aparelho, declarado no PR: aparência real do tema claro, troca do modo do sistema com o app aberto e duração real da Splash na partida a frio.
