package com.br.androideventscheduler.app

import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.verify

/** Falha se algum módulo registrado em [appModules] tiver dependência sem definição. Cresce a cada spec. */
@OptIn(KoinExperimentalAPI::class)
class KoinModulesVerificationTest {
    @Test
    fun `given all registered modules when verifying the graph then every definition resolves`() {
        appModules.forEach { module -> module.verify() }
    }
}
