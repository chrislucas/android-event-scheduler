package com.br.androideventscheduler.core.ui

/**
 * Tags de teste/semântica ([androidx.compose.ui.platform.testTag]) de todas as telas.
 * Cada spec acrescenta as suas constantes, agrupadas por tela.
 */
object SemanticsTags {
    // Placeholders temporários do AppNavHost (cada spec remove o seu)
    const val HOME_PLACEHOLDER = "home_placeholder" // removido pela spec 05

    // Splash
    const val SPLASH_LOGO = "splash_logo"
    const val SPLASH_TITLE = "splash_title"

    // Configurações
    const val SETTINGS_BACK_BUTTON = "settings_back_button"
    const val SETTINGS_LOADING = "settings_loading"
    const val SETTINGS_ACCOUNT_EMAIL_INPUT = "settings_account_email_input"
    const val SETTINGS_REMINDER_OPTION_PREFIX = "settings_reminder_option_"
    const val SETTINGS_SNOOZE_OPTION_PREFIX = "settings_snooze_option_"
    const val SETTINGS_FULLSCREEN_SWITCH = "settings_fullscreen_switch"
    const val SETTINGS_SAVE_BUTTON = "settings_save_button"
}
