package com.br.androideventscheduler.core.ui

/**
 * Tags de teste/semântica ([androidx.compose.ui.platform.testTag]) de todas as telas.
 * Cada spec acrescenta as suas constantes, agrupadas por tela.
 */
object SemanticsTags {
    // Placeholders temporários do AppNavHost (cada spec remove o seu)
    const val UPSERT_PLACEHOLDER = "upsert_placeholder" // removido pela spec 07

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

    // Detalhes (spec 10)
    const val DETAIL_TITLE = "detail_title"
    const val DETAIL_DATE_RANGE = "detail_date_range"
    const val DETAIL_ACCOUNT_EMAIL = "detail_account_email"
    const val DETAIL_REMINDER_LABEL = "detail_reminder_label"
    const val DETAIL_RECURRENCE_LABEL = "detail_recurrence_label"
    const val DETAIL_LOCATION = "detail_location"
    const val DETAIL_NOTES = "detail_notes"
    const val DETAIL_BACK_BUTTON = "detail_back_button"
    const val DETAIL_EDIT_BUTTON = "detail_edit_button"
    const val DETAIL_LOADING = "detail_loading"
    const val DETAIL_NOT_FOUND = "detail_not_found"
    const val DETAIL_ACTION_BAR = "detail_action_bar"

    // Pesquisa (spec 13)
    const val SEARCH_QUERY_INPUT = "search_query_input"
    const val SEARCH_CLEAR_BUTTON = "search_clear_button"
    const val SEARCH_BACK_BUTTON = "search_back_button"
    const val SEARCH_FILTER_CHIP_PREFIX = "search_filter_chip_"
    const val SEARCH_RESULT_ITEM_PREFIX = "search_result_item_"
    const val SEARCH_RESULTS_COUNT = "search_results_count"
    const val SEARCH_EMPTY_STATE = "search_empty_state"
    const val SEARCH_HINT = "search_hint"
    const val SEARCH_ERROR_STATE = "search_error_state"
    const val SEARCH_RETRY_BUTTON = "search_retry_button"
    const val SEARCH_LOADING = "search_loading"

    // Home (spec 05)
    const val HOME_MONTH_HEADER = "home_month_header"
    const val HOME_SEARCH_BUTTON = "home_search_button"
    const val HOME_PREVIOUS_MONTH_BUTTON = "home_previous_month_button"
    const val HOME_NEXT_MONTH_BUTTON = "home_next_month_button"
    const val HOME_CALENDAR_GRID = "home_calendar_grid"
    const val HOME_DAY_CELL_PREFIX = "home_day_cell_"
    const val HOME_DAY_HEADER = "home_day_header"
    const val HOME_SCHEDULE_LIST = "home_schedule_list"
    const val HOME_SCHEDULE_ITEM_PREFIX = "home_schedule_item_"
    const val HOME_EMPTY_DAY = "home_empty_day"
    const val HOME_QUICK_ADD_BAR = "home_quick_add_bar"
    const val HOME_MONTH_PICKER_SHEET = "home_month_picker_sheet"
    const val HOME_MONTH_OPTION_PREFIX = "home_month_option_"
    const val HOME_PICKER_PREVIOUS_YEAR_BUTTON = "home_picker_previous_year_button"
    const val HOME_PICKER_NEXT_YEAR_BUTTON = "home_picker_next_year_button"
    const val HOME_PICKER_YEAR = "home_picker_year"
    const val HOME_LOADING = "home_loading"
    const val HOME_ERROR_STATE = "home_error_state"
    const val HOME_RETRY_BUTTON = "home_retry_button"
}
