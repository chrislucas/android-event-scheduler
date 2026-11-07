package com.br.androideventscheduler.core.schedule.domain.model

/** Paleta de cores dos itens. O nome exibido é da apresentação (resource). */
enum class EventColorOption(
    val hex: Long,
) {
    LIGHT_BLUE(0xFF29B6F6),
    INDIGO(0xFF5C6BC0),
    TEAL(0xFF26A69A),
    OLIVE_GREEN(0xFF9CCC65),
    PURPLE(0xFFAB47BC),
    CORAL(0xFFEF5350),
    ;

    companion object {
        fun fromHex(hex: Long): EventColorOption = entries.firstOrNull { it.hex == hex } ?: LIGHT_BLUE
    }
}

/** Catálogo de adesivos. O rótulo de acessibilidade é da apresentação (resource). */
enum class StickerEmoji(
    val emoji: String,
) {
    SMILE("😊"),
    STUDY("📚"),
    CODE("💻"),
    ROCKET("🚀"),
    STAR("⭐"),
    PARTY("🎉"),
    WORKOUT("🥋"),
    COFFEE("☕"),
}
