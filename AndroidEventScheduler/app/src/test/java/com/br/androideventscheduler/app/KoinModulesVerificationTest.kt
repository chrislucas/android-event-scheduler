package com.br.androideventscheduler.app

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.SavedStateHandle
import com.br.androideventscheduler.core.security.di.securityModule
import com.br.androideventscheduler.feature.auth.di.authModule
import com.br.androideventscheduler.feature.backup.di.backupModule
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Falha se alguma definição de [appModules] tiver dependência sem definição. Verifica o grafo
 * **agregado** (como em produção), pois módulos de feature dependem de definições do `core`.
 * Cresce sozinho a cada módulo adicionado em [appModules].
 */
@OptIn(KoinExperimentalAPI::class)
class KoinModulesVerificationTest {
    // Tipos do framework entregues por `androidContext()`/serviços do sistema, que o verify não enxerga como definições.
    private val androidFrameworkTypes =
        listOf(
            Context::class,
            AlarmManager::class,
            NotificationManager::class,
            // Resources: entregue por androidContext() (mapper da pesquisa).
            Resources::class,
        )

    // Tipos fornecidos em runtime (rota/`parametersOf` e Koin ao criar ViewModels), não por um módulo.
    private val runtimeParameterTypes =
        listOf(
            String::class,
            // Long: data inicial da rota (UpsertScheduleViewModel); () -> String: idProvider com valor padrão.
            Long::class,
            Function0::class,
            // SavedStateHandle: injetado pelo Koin ao criar ViewModels.
            SavedStateHandle::class,
        )

    @Test
    fun `given all registered modules when verifying the aggregated graph then every definition resolves`() {
        module { includes(appModules) }.verify(extraTypes = androidFrameworkTypes + runtimeParameterTypes)
    }

    // Importar/exportar (spec 17)
    @Test
    fun `given the backup feature when listing the aggregated modules then its modules are registered`() {
        assertTrue(backupModule in appModules)
        assertTrue(incomingFileModule in appModules)
    }

    // Bloqueio do app (spec 16)
    @Test
    fun `given the app lock feature when listing the aggregated modules then its modules are registered`() {
        assertTrue(securityModule in appModules)
        assertTrue(authModule in appModules)
    }
}
