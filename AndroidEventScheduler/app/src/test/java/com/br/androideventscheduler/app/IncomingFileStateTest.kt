package com.br.androideventscheduler.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomingFileStateTest {
    private val state = IncomingFileState()

    @Test
    fun `given nothing registered when reading the pending uri then it is null`() {
        assertNull(state.pendingFileUri.value)
    }

    @Test
    fun `given a uri when setting then it becomes the pending uri`() {
        state.set("content://mail/a.json")

        assertEquals("content://mail/a.json", state.pendingFileUri.value)
    }

    @Test
    fun `given null or blank uris when setting then the pending uri is unchanged`() {
        state.set("content://mail/a.json")

        state.set(null)
        state.set("  ")

        assertEquals("content://mail/a.json", state.pendingFileUri.value)
    }

    @Test
    fun `given a pending uri when consuming it twice then it is cleared and stays cleared`() {
        state.set("content://mail/a.json")

        state.consume("content://mail/a.json")
        state.consume("content://mail/a.json")

        assertNull(state.pendingFileUri.value)
    }

    @Test
    fun `given a newer uri arrived when consuming the older one then the newer uri is kept`() {
        state.set("content://mail/old.json")
        state.set("content://mail/new.json")

        state.consume("content://mail/old.json")

        assertEquals("content://mail/new.json", state.pendingFileUri.value)
    }
}
