package com.br.androideventscheduler.feature.backup.data.codec

import com.br.androideventscheduler.core.schedule.domain.model.ScheduleItem
import com.br.androideventscheduler.feature.backup.data.dto.AgendaFileDto
import com.br.androideventscheduler.feature.backup.data.dto.ScheduleItemDto
import com.br.androideventscheduler.feature.backup.data.mapper.AgendaDtoMapper
import com.br.androideventscheduler.feature.backup.domain.model.BackupError
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import java.time.Instant

/** Itens válidos de um arquivo + quantos foram ignorados (inválidos ou com `id` repetido dentro do arquivo). */
internal data class DecodedAgenda(
    val items: List<ScheduleItem>,
    val invalidCount: Int,
)

/**
 * Serialização do arquivo de backup. A leitura valida em duas camadas: (1) estrutural — JSON, `format`, `version` e
 * `items` — cuja falha recusa o arquivo inteiro com um [BackupError]; (2) por item — itens inválidos são contados e
 * ignorados. `ignoreUnknownKeys` permite ler arquivos de versões futuras compatíveis.
 */
internal class AgendaJsonCodec {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

    fun encode(
        items: List<ScheduleItem>,
        exportedAt: Instant,
        appVersion: String,
    ): String =
        json.encodeToString(
            AgendaFileDto.serializer(),
            AgendaFileDto(
                format = AgendaFileFormat.FORMAT_ID,
                version = AgendaFileFormat.CURRENT_VERSION,
                exportedAt = exportedAt.toString(),
                appVersion = appVersion,
                items = items.map(AgendaDtoMapper::toDto),
            ),
        )

    /** @throws BackupError.NotRememberMeFile, BackupError.UnsupportedVersion ou BackupError.Corrupted. */
    fun decode(text: String): DecodedAgenda {
        val root = parseRoot(text)
        requireRememberMeFormat(root)
        requireSupportedVersion(root)
        val rawItems = root["items"] as? JsonArray ?: throw BackupError.Corrupted

        val itemsById = LinkedHashMap<String, ScheduleItem>()
        var invalidCount = 0
        rawItems.forEach { element ->
            val item = decodeItem(element)
            // Mesmo id duas vezes no arquivo: vale o último e o anterior conta como ignorado.
            if (item == null || itemsById.put(item.id, item) != null) invalidCount++
        }
        return DecodedAgenda(items = itemsById.values.toList(), invalidCount = invalidCount)
    }

    private fun requireRememberMeFormat(root: JsonObject) {
        val formatId = (root["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (formatId != AgendaFileFormat.FORMAT_ID) throw BackupError.NotRememberMeFile
    }

    private fun requireSupportedVersion(root: JsonObject) {
        val version =
            (root["version"] as? JsonPrimitive)
                ?.takeUnless { it.isString }
                ?.intOrNull
                ?.takeIf { it >= 1 }
                ?: throw BackupError.Corrupted
        if (version > AgendaFileFormat.CURRENT_VERSION) throw BackupError.UnsupportedVersion(version)
    }

    private fun parseRoot(text: String): JsonObject = parseElement(text) as? JsonObject ?: throw BackupError.NotRememberMeFile

    private fun parseElement(text: String): JsonElement {
        // O parser de árvore é recursivo: aninhamento enorme estoura a pilha (Error, não Exception) e derrubaria o app.
        if (exceedsStructuralLimits(text)) throw BackupError.Corrupted
        return try {
            json.parseToJsonElement(text)
        } catch (_: SerializationException) {
            throw BackupError.Corrupted
        }
    }

    private fun decodeItem(element: JsonElement): ScheduleItem? {
        val dto =
            try {
                json.decodeFromJsonElement<ScheduleItemDto>(element)
            } catch (_: SerializationException) {
                return null
            }
        return AgendaDtoMapper.toDomainOrNull(dto)
    }

    /**
     * Varre o texto (fora de strings) e recusa aninhamento acima de [AgendaFileFormat.MAX_NESTING_DEPTH] ou mais de
     * [AgendaFileFormat.MAX_JSON_NODES] valores/contêineres: o parser monta a árvore inteira na memória e um arquivo de
     * 5 MB de `[],[],[]...` ou `1,1,1...` ocuparia dezenas de vezes seu tamanho. O formato real usa 5 níveis.
     */
    private fun exceedsStructuralLimits(text: String): Boolean {
        var depth = 0
        var nodes = 0
        var inString = false
        var escaped = false
        for (char in text) {
            when {
                escaped -> {
                    escaped = false
                }

                inString -> {
                    if (char == '\\') {
                        escaped = true
                    } else if (char == '"') {
                        inString = false
                    }
                }

                char == '"' -> {
                    inString = true
                }

                char == '{' || char == '[' -> {
                    depth++
                    nodes++
                }

                char == '}' || char == ']' -> {
                    depth--
                }

                char == ',' -> {
                    nodes++
                }
            }
            if (depth > AgendaFileFormat.MAX_NESTING_DEPTH || nodes > AgendaFileFormat.MAX_JSON_NODES) return true
        }
        return false
    }
}
