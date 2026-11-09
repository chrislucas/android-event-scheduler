package com.br.androideventscheduler.feature.backup.data.codec

import com.br.androideventscheduler.feature.backup.data.fileJson
import com.br.androideventscheduler.feature.backup.data.fullyPopulatedItems
import com.br.androideventscheduler.feature.backup.data.itemJson
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.Instant

class AgendaJsonCodecTest {
    private val codec = AgendaJsonCodec()
    private val exportedAt = Instant.parse("2026-11-08T12:00:00Z")

    private fun decodeFailure(text: String): BackupError {
        try {
            codec.decode(text)
        } catch (error: BackupError) {
            return error
        }
        fail("expected a BackupError")
        error("unreachable")
    }

    @Test
    fun `given items with every field and recurrence pattern when encoding and decoding then nothing is lost`() {
        val items = fullyPopulatedItems()

        val decoded = codec.decode(codec.encode(items, exportedAt, appVersion = "1.0"))

        assertEquals(items, decoded.items)
        assertEquals(0, decoded.invalidCount)
    }

    @Test
    fun `given encoded agenda when reading the header then format version and metadata are written`() {
        val text = codec.encode(fullyPopulatedItems(), exportedAt, appVersion = "1.2.3")

        assertTrue(text.contains("\"format\":\"rememberme-agenda\""))
        assertTrue(text.contains("\"version\":1"))
        assertTrue(text.contains("\"exportedAt\":\"2026-11-08T12:00:00Z\""))
        assertTrue(text.contains("\"appVersion\":\"1.2.3\""))
    }

    @Test
    fun `given encoded agenda when reading enums then they are written by stable name not ordinal`() {
        val text = codec.encode(fullyPopulatedItems(), exportedAt, appVersion = "1.0")

        assertTrue(text.contains("\"type\":\"REMINDER\""))
        assertTrue(text.contains("\"calendarSource\":\"family_calendar\""))
        assertTrue(text.contains("\"reminderOffset\":\"THIRTY_MINUTES\""))
        assertTrue(text.contains("\"daysOfWeek\":[\"MONDAY\",\"FRIDAY\"]"))
    }

    @Test
    fun `given empty agenda when encoding and decoding then it round trips to no items`() {
        val decoded = codec.decode(codec.encode(emptyList(), exportedAt, appVersion = "1.0"))

        assertTrue(decoded.items.isEmpty())
        assertEquals(0, decoded.invalidCount)
    }

    @Test
    fun `given unknown keys in header and item when decoding then they are ignored`() {
        val text =
            """{"format":"rememberme-agenda","version":1,"futureField":{"a":1},"items":[${itemJson(extra = ""","novo":[1,2]""")}]}"""

        val decoded = codec.decode(text)

        assertEquals(listOf("i1"), decoded.items.map { it.id })
    }

    @Test
    fun `given wrong format when decoding then it is not a rememberme file`() {
        assertEquals(BackupError.NotRememberMeFile, decodeFailure(fileJson(itemJson(), format = "\"outro-app\"")))
    }

    @Test
    fun `given valid json without format or not an object when decoding then it is not a rememberme file`() {
        assertEquals(BackupError.NotRememberMeFile, decodeFailure("""{"items":[]}"""))
        assertEquals(BackupError.NotRememberMeFile, decodeFailure("[1,2,3]"))
        assertEquals(BackupError.NotRememberMeFile, decodeFailure("42"))
    }

    @Test
    fun `given a newer version when decoding then it is reported as unsupported with that version`() {
        assertEquals(BackupError.UnsupportedVersion(2), decodeFailure(fileJson(itemJson(), version = "2")))
    }

    @Test
    fun `given missing or invalid version when decoding then the file is corrupted`() {
        assertEquals(BackupError.Corrupted, decodeFailure("""{"format":"rememberme-agenda","items":[]}"""))
        assertEquals(BackupError.Corrupted, decodeFailure(fileJson("", version = "\"1\"")))
        assertEquals(BackupError.Corrupted, decodeFailure(fileJson("", version = "0")))
        assertEquals(BackupError.Corrupted, decodeFailure(fileJson("", version = "1.5")))
    }

    @Test
    fun `given truncated or non json text when decoding then the file is corrupted`() {
        val valid = fileJson(itemJson())

        assertEquals(BackupError.Corrupted, decodeFailure(valid.dropLast(10)))
        assertEquals(BackupError.Corrupted, decodeFailure(""))
        assertEquals(BackupError.Corrupted, decodeFailure("isso não é json"))
    }

    @Test
    fun `given items that is not an array when decoding then the file is corrupted`() {
        assertEquals(
            BackupError.Corrupted,
            decodeFailure("""{"format":"rememberme-agenda","version":1,"items":"nope"}"""),
        )
    }

    @Test
    fun `given extremely nested json when decoding then it fails safely instead of crashing`() {
        val depth = 200_000
        val text = "[".repeat(depth) + "]".repeat(depth)

        val error = decodeFailure(text)

        assertTrue(error is BackupError.NotRememberMeFile || error is BackupError.Corrupted)
    }

    @Test
    fun `given a file with millions of tiny values when decoding then it is refused before building the tree`() {
        val flood = "{\"format\":\"rememberme-agenda\",\"version\":1,\"items\":[" + "[],".repeat(1_100_000) + "[]]}"

        assertEquals(BackupError.Corrupted, decodeFailure(flood))
    }

    @Test
    fun `given a realistic large agenda when decoding then the structural limit does not refuse it`() {
        val items = List(10_000) { index -> itemJson(id = "bulk-$index", extra = ""","location":"Sala $index","notes":"x"""") }
        val text = fileJson(items.joinToString(","))

        val decoded = codec.decode(text)

        assertEquals(10_000, decoded.items.size)
    }

    @Test
    fun `given brackets and escaped quotes inside strings when decoding then they do not count as nesting`() {
        val title = "\"" + "[{".repeat(100) + "\\\"[" + "\""

        val decoded = codec.decode(fileJson(itemJson(title = title)))

        assertEquals(1, decoded.items.size)
        assertTrue(
            decoded.items
                .single()
                .title
                .startsWith("[{[{"),
        )
    }

    @Test
    fun `given invalid and valid items when decoding then invalid ones are ignored and counted`() {
        val items =
            listOf(
                itemJson(id = "ok-1"),
                itemJson(id = "bad-date", startDate = "\"2026-13-40\""),
                """{"id":"missing-fields"}""",
                "\"not an object\"",
                itemJson(id = "ok-2"),
            ).joinToString(",")

        val decoded = codec.decode(fileJson(items))

        assertEquals(listOf("ok-1", "ok-2"), decoded.items.map { it.id })
        assertEquals(3, decoded.invalidCount)
    }

    @Test
    fun `given the same id twice in the file when decoding then the last one wins and the earlier is counted as ignored`() {
        val items = listOf(itemJson(id = "dup", title = "\"Primeiro\""), itemJson(id = "dup", title = "\"Segundo\"")).joinToString(",")

        val decoded = codec.decode(fileJson(items))

        assertEquals(listOf("Segundo"), decoded.items.map { it.title })
        assertEquals(1, decoded.invalidCount)
    }

    @Test
    fun `given the documented example when decoding then it is read successfully`() {
        val markdown = File("../docs/formato-agenda-json.md").readText()
        val example = markdown.substringAfter("```json").substringBefore("```").trim()

        val decoded = codec.decode(example)

        assertTrue(decoded.items.isNotEmpty())
        assertEquals(0, decoded.invalidCount)
    }
}
