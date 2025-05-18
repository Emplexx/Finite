package moe.emi.finite.components.colors

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import moe.emi.finite.dump.isDarkTheme

object Contrast {
	const val NormalAAA = 7.0
	const val NormalAA = 4.5
	const val LargeAAA = 4.5
	const val LargeAA = 3.0
}

enum class PaletteTone {
	Light, Dark, DeviceTheme;
	
	fun getMatchingStatusBarColor(context: Context): Boolean =
		when (this) {
			DeviceTheme -> !context.isDarkTheme
			Dark -> true
			Light -> false
		}
}

enum class ContentTone {
	Light, Dark;
	
	val correspondingColor: Int
		get() = when (this) {
			Light -> Color.WHITE
			Dark -> Color.BLACK
		}
	val opposite: ContentTone
		get() = when (this) {
			Light -> Dark
			Dark -> Light
		}
	val correspondingPaletteTone: PaletteTone
		get() = when (this) {
			Light -> PaletteTone.Light
			Dark -> PaletteTone.Dark
		}
}

/**
 * Returns the given [preferredTone] [ContentTone] if it meets the contrast [threshold] on the given [color], and the opposite [ContentTone] otherwise
 */
fun calculateContentColorWithThreshold(
	color: Int,
	preferredTone: ContentTone,
	threshold: Double,
) = ColorUtils
	.calculateContrast(
		preferredTone.correspondingColor,
		color,
	)
	.let { ratio ->
		val passesThreshold = ratio > threshold
		if (passesThreshold) preferredTone else preferredTone.opposite
	}

/**
 * Returns a [ContentTone] that scores higher contrast on the given [color]
 */
fun calculateContentColorNeutral(color: Int) =
	listOf(
		ContentTone.Light to ColorUtils.calculateContrast(Color.WHITE, color),
		ContentTone.Dark to ColorUtils.calculateContrast(Color.BLACK, color)
	)
		.maxBy { it.second }
		.first