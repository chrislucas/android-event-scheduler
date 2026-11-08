package com.br.androideventscheduler.core.common.testing

import org.junit.Assert.assertEquals
import org.junit.Test

class FakeStringProviderTest {
    private val provider = FakeStringProvider()

    @Test
    fun `given id without args when getting string then it returns only the id`() {
        assertEquals("string(7)", provider.getString(7))
    }

    @Test
    fun `given args when getting string then they are appended in order`() {
        assertEquals("string(7)[a, 2]", provider.getString(7, "a", 2))
    }

    @Test
    fun `given quantity when getting plural then quantity and args are included`() {
        assertEquals("plural(9,5)[5]", provider.getQuantityString(9, 5, 5))
    }
}
