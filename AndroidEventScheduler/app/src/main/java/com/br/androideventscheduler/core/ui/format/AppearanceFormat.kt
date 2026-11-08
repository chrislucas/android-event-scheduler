package com.br.androideventscheduler.core.ui.format

import androidx.annotation.StringRes
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.schedule.domain.model.EventColorOption
import com.br.androideventscheduler.core.schedule.domain.model.StickerEmoji

/** Rótulo de acessibilidade do adesivo (o domínio não conhece `R`). */
@StringRes
fun StickerEmoji.labelRes(): Int =
    when (this) {
        StickerEmoji.SMILE -> R.string.sticker_label_smile
        StickerEmoji.STUDY -> R.string.sticker_label_study
        StickerEmoji.CODE -> R.string.sticker_label_code
        StickerEmoji.ROCKET -> R.string.sticker_label_rocket
        StickerEmoji.STAR -> R.string.sticker_label_star
        StickerEmoji.PARTY -> R.string.sticker_label_party
        StickerEmoji.WORKOUT -> R.string.sticker_label_workout
        StickerEmoji.COFFEE -> R.string.sticker_label_coffee
    }

/** Nome da cor, anunciado pelo leitor de tela (a cor nunca é a única pista). */
@StringRes
fun EventColorOption.labelRes(): Int =
    when (this) {
        EventColorOption.LIGHT_BLUE -> R.string.color_label_light_blue
        EventColorOption.INDIGO -> R.string.color_label_indigo
        EventColorOption.TEAL -> R.string.color_label_teal
        EventColorOption.OLIVE_GREEN -> R.string.color_label_olive_green
        EventColorOption.PURPLE -> R.string.color_label_purple
        EventColorOption.CORAL -> R.string.color_label_coral
    }
