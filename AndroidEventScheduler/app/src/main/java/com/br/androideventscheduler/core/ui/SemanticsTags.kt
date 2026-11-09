package com.br.androideventscheduler.core.ui

/**
 * Tags de teste/semântica ([androidx.compose.ui.platform.testTag]) de todas as telas.
 * Cada spec acrescenta as suas constantes, agrupadas por tela.
 */
object SemanticsTags {
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

    // Duplicar/excluir (spec 11)
    const val DETAIL_DUPLICATE_BUTTON = "detail_duplicate_button"
    const val DETAIL_DELETE_BUTTON = "detail_delete_button"
    const val DETAIL_SNACKBAR_HOST = "detail_snackbar_host"
    const val DELETE_CONFIRM_DIALOG = "delete_confirm_dialog"
    const val DELETE_CONFIRM_BUTTON = "delete_confirm_button"
    const val DELETE_CANCEL_BUTTON = "delete_cancel_button"

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

    // Criar/editar (spec 07)
    const val UPSERT_MODE_EVENT_TAB = "upsert_mode_event_tab"
    const val UPSERT_MODE_REMINDER_TAB = "upsert_mode_reminder_tab"
    const val UPSERT_TITLE_INPUT = "upsert_title_input"
    const val UPSERT_ALL_DAY_SWITCH = "upsert_all_day_switch"
    const val UPSERT_DATE_SELECTOR_ROW = "upsert_date_selector_row"
    const val UPSERT_END_DATE_SELECTOR_ROW = "upsert_end_date_selector_row"
    const val UPSERT_INLINE_CALENDAR = "upsert_inline_calendar"
    const val UPSERT_TIME_SELECTOR_ROW = "upsert_time_selector_row"
    const val UPSERT_END_TIME_SELECTOR_ROW = "upsert_end_time_selector_row"
    const val UPSERT_INLINE_TIME_PICKER = "upsert_inline_time_picker"
    const val UPSERT_QUICK_CHIP_PREFIX = "upsert_quick_chip_"
    const val UPSERT_REMINDER_ROW = "upsert_reminder_row"
    const val UPSERT_REMINDER_OPTION_PREFIX = "upsert_reminder_option_"
    const val UPSERT_CALENDAR_SOURCE_ROW = "upsert_calendar_source_row"
    const val UPSERT_CALENDAR_SOURCE_OPTION_PREFIX = "upsert_calendar_source_option_"
    const val UPSERT_LOCATION_INPUT = "upsert_location_input"
    const val UPSERT_NOTES_INPUT = "upsert_notes_input"
    const val UPSERT_RECURRENCE_ROW = "upsert_recurrence_row"
    const val UPSERT_SAVE_BUTTON = "upsert_save_button"
    const val UPSERT_CANCEL_BUTTON = "upsert_cancel_button"
    const val UPSERT_LOADING = "upsert_loading"

    // Tela cheia (spec 14)
    const val ALARM_FULLSCREEN_LOADING = "alarm_fullscreen_loading"
    const val ALARM_FULLSCREEN_BADGE = "alarm_fullscreen_badge"
    const val ALARM_FULLSCREEN_TITLE = "alarm_fullscreen_title"
    const val ALARM_FULLSCREEN_TIME = "alarm_fullscreen_time"
    const val ALARM_FULLSCREEN_LOCATION = "alarm_fullscreen_location"
    const val ALARM_FULLSCREEN_NOTES = "alarm_fullscreen_notes"
    const val ALARM_FULLSCREEN_ERROR = "alarm_fullscreen_error"
    const val ALARM_SNOOZE_CHIPS = "alarm_snooze_chips"
    const val ALARM_SNOOZE_CHIP_PREFIX = "alarm_snooze_chip_"
    const val ALARM_FULLSCREEN_SNOOZE_BUTTON = "alarm_fullscreen_snooze_button"
    const val ALARM_FULLSCREEN_DISMISS_BUTTON = "alarm_fullscreen_dismiss_button"

    // Compartilhar (spec 12)
    const val DETAIL_SHARE_BUTTON = "detail_share_button"

    // Recorrência (spec 08)
    const val RECURRENCE_DIALOG = "recurrence_dialog"
    const val RECURRENCE_OPTION_NONE = "recurrence_option_none"
    const val RECURRENCE_OPTION_DAILY = "recurrence_option_daily"
    const val RECURRENCE_OPTION_WEEKLY = "recurrence_option_weekly"
    const val RECURRENCE_DAY_TOGGLE_PREFIX = "recurrence_day_toggle_"
    const val RECURRENCE_COUNT_INPUT = "recurrence_count_input"
    const val RECURRENCE_CONFIRM_BUTTON = "recurrence_confirm_button"
    const val RECURRENCE_DISMISS_BUTTON = "recurrence_dismiss_button"

    // Gaveta da Home (spec 06)
    const val HOME_MENU_BUTTON = "home_menu_button"
    const val HOME_DRAWER_SHEET = "home_drawer_sheet"
    const val HOME_DRAWER_FILTER_PREFIX = "home_drawer_filter_"
    const val HOME_DRAWER_SETTINGS_BUTTON = "home_drawer_settings_button"

    // Seletor de cor e emoji (spec 09)
    const val STICKER_PICKER_DIALOG = "sticker_picker_dialog"
    const val STICKER_OPTION_PREFIX = "sticker_option_"
    const val STICKER_CLEAR_BUTTON = "sticker_clear_button"
    const val COLOR_PICKER_DIALOG = "color_picker_dialog"
    const val COLOR_OPTION_PREFIX = "color_option_"
    const val COLOR_OPTION_CHECK_PREFIX = "color_option_check_"
    const val UPSERT_STICKER_BUTTON = "upsert_sticker_button"
    const val UPSERT_COLOR_BUTTON = "upsert_color_button"
    const val HOME_DAY_STICKER_BUTTON = "home_day_sticker_button"

    /** `home_day_indicator_<epochDay>_<colorHex>`: a cor desenhada vai na tag (pixels ilegíveis no Robolectric). */
    const val HOME_DAY_INDICATOR_PREFIX = "home_day_indicator_"

    /** `home_schedule_color_bar_<id>_<colorHex>`. */
    const val HOME_SCHEDULE_COLOR_BAR_PREFIX = "home_schedule_color_bar_"

    // Importar/exportar agenda (spec 17)
    const val SETTINGS_BACKUP_ITEM = "settings_backup_item"
    const val BACKUP_BACK_BUTTON = "backup_back_button"
    const val BACKUP_LOADING = "backup_loading"
    const val BACKUP_EXPORT_SHARE_BUTTON = "backup_export_share_button"
    const val BACKUP_EXPORT_SAVE_BUTTON = "backup_export_save_button"
    const val BACKUP_EXPORT_EMPTY_MESSAGE = "backup_export_empty_message"
    const val BACKUP_EXPORT_PROGRESS = "backup_export_progress"
    const val BACKUP_ENCRYPTION_WARNING = "backup_encryption_warning"
    const val BACKUP_IMPORT_PICK_BUTTON = "backup_import_pick_button"
    const val BACKUP_IMPORT_PROGRESS = "backup_import_progress"
    const val BACKUP_IMPORT_ERROR = "backup_import_error"
    const val BACKUP_IMPORT_PREVIEW = "backup_import_preview"
    const val BACKUP_IMPORT_PREVIEW_TOTAL = "backup_import_preview_total"
    const val BACKUP_IMPORT_PREVIEW_NEW = "backup_import_preview_new"
    const val BACKUP_IMPORT_PREVIEW_EXISTING = "backup_import_preview_existing"
    const val BACKUP_IMPORT_PREVIEW_INVALID = "backup_import_preview_invalid"
    const val BACKUP_IMPORT_STRATEGY_GROUP = "backup_import_strategy_group"
    const val BACKUP_IMPORT_STRATEGY_OPTION_PREFIX = "backup_import_strategy_option_"
    const val BACKUP_IMPORT_CONFIRM_BUTTON = "backup_import_confirm_button"
    const val BACKUP_IMPORT_CANCEL_BUTTON = "backup_import_cancel_button"

    // Bloqueio do app (spec 16)
    const val LOCK_SCREEN = "lock_screen"
    const val LOCK_UNLOCK_BUTTON = "lock_unlock_button"
    const val LOCK_STATUS_MESSAGE = "lock_status_message"
    const val LOCK_PROGRESS = "lock_progress"
    const val SETTINGS_APP_LOCK_SWITCH = "settings_app_lock_switch"
    const val SETTINGS_APP_LOCK_DISCLAIMER = "settings_app_lock_disclaimer"
    const val SETTINGS_APP_LOCK_INSECURE_HINT = "settings_app_lock_insecure_hint"
    const val SETTINGS_APP_LOCK_OPEN_SECURITY_BUTTON = "settings_app_lock_open_security_button"
    const val SETTINGS_APP_LOCK_TIMEOUT_OPTION_PREFIX = "settings_app_lock_timeout_option_"
}
