package com.br.androideventscheduler.core.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.br.androideventscheduler.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "pt-rBR")
class AndroidStringProviderRobolectricTest {
    private val provider: StringProvider = AndroidStringProvider(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `given formatted string resource when getting string then arguments are substituted`() {
        assertEquals("Detalhes: ok", provider.getString(R.string.detail_row_description, "Detalhes", "ok"))
    }

    @Test
    fun `given plural resource when getting quantity string then singular and plural are chosen`() {
        assertEquals("1 minuto antes", provider.getQuantityString(R.plurals.reminder_minutes_before, 1, 1))
        assertEquals("10 minutos antes", provider.getQuantityString(R.plurals.reminder_minutes_before, 10, 10))
    }
}
