package com.br.androideventscheduler.core.security.domain

/** Tempo em segundo plano a partir do qual o app volta a pedir a credencial. */
enum class AppLockTimeout(
    val durationMillis: Long,
) {
    IMMEDIATE(0L),
    ONE_MINUTE(60_000L),
    FIVE_MINUTES(300_000L),
    ;

    companion object {
        val DEFAULT = ONE_MINUTE
    }
}
