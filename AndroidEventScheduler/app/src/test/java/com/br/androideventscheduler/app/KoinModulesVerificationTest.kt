package com.br.androideventscheduler.app

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
    @Test
    fun `given all registered modules when verifying the aggregated graph then every definition resolves`() {
        module { includes(appModules) }.verify()
    }
}
