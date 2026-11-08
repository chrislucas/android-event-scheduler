package com.br.androideventscheduler.feature.upsert.domain.model

/** Erros de validação ao salvar um agendamento; a apresentação os mapeia para resources. */
sealed class SaveScheduleError : Exception() {
    /** O título ficou vazio após `trim`. */
    data object BlankTitle : SaveScheduleError() {
        private fun readResolve(): Any = BlankTitle
    }

    /** O fim (data e hora) é anterior ao início. */
    data object EndBeforeStart : SaveScheduleError() {
        private fun readResolve(): Any = EndBeforeStart
    }
}
