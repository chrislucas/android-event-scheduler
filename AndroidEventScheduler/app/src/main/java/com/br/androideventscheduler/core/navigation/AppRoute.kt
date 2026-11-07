package com.br.androideventscheduler.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** Rotas do app (Navigation 3). Vivem em `core` para que as features se referenciem sem se importar. */
@Serializable
data object Splash : NavKey

@Serializable
data object Home : NavKey

@Serializable
data object Search : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data class ScheduleDetail(
    val scheduleId: String,
) : NavKey

@Serializable
data class UpsertSchedule(
    val scheduleId: String? = null,
    val initialDateEpochDay: Long? = null,
) : NavKey
