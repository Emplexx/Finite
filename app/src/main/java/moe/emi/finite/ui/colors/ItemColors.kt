package moe.emi.finite.ui.colors

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.finite.dump.alpha
import moe.emi.finite.dump.isDarkTheme
import moe.emi.finite.service.datastore.appSettings

data class ItemColors(
	val source: Int,
	val container: Int,
	val onContainer: Int,
	val onContainerVariant: Int,
	val tone: PaletteTone,
) {
	constructor(context: Context) : this(
		source = context.materialColor(TonalColor.surfaceVariant),
		container = context.materialColor(TonalColor.surfaceVariant),
		onContainer = context.materialColor(TonalColor.onSurface),
		onContainerVariant = context.materialColor(TonalColor.onSurface).alpha(0.54f),
		tone = PaletteTone.DeviceTheme
	)
}

@SuppressLint("RestrictedApi")
fun Context.makeItemColors(source: Int?): ItemColors {
	
	source ?: return ItemColors(this)
	
	// TODO make function suspend?
	val settings = runBlocking { appSettings.first() }
	
	val harmonize = settings.harmonizeColors
	val normalize = settings.normalizeColors

	// ==
	
	val harmonizedSource = if (!harmonize) source
	else Blend.harmonize(source, materialColor(TonalColor.primary))
	
	if (normalize) {
		val tokens = colorTokens
		val palette = TonalPalette.fromInt(harmonizedSource)
		
		return ItemColors(
			source = source,
			container = palette.tone(tokens.container),
			onContainer = palette.tone(tokens.onContainer),
			onContainerVariant = palette.tone(tokens.onContainer).alpha(0.54f),
			tone = PaletteTone.DeviceTheme
		)
	} else {
		
		val hct = Hct.fromInt(harmonizedSource)
//		val onContainer = if (hct.tone < 65) Color.WHITE
//		else Hct.fromInt(harmonizedSource).also { it.tone = 10.0 }.toInt()

		val contentTone =
//			calculateContentColorNeutral(hct.toInt())
			calculateContentColorWithThreshold(
				hct.toInt(),
				ContentTone.Light, Contrast.LargeAA
			)
		
		
		val (onContainer1, onContainerVariant1) =
			 ColorUtils.calculateContrast(Color.WHITE, hct.toInt())
//			calculateForegroundColor(hct.toInt())
			.let { result ->
				
				val whiteHasMoreContrast = result > Contrast.LargeAA
//				val whiteHasMoreContrast = result == Color.WHITE
				
				if (whiteHasMoreContrast) Color.WHITE to Color.WHITE.alpha(0.6f)
				else Hct.fromInt(harmonizedSource)
					.also { it.tone = 10.0 }.toInt()
					.let { it to it.alpha(0.54f) }
			}
		
		val (onContainer, onContainerVariant) = when (contentTone) {
			ContentTone.Light -> Color.WHITE to Color.WHITE.alpha(0.6f)
			ContentTone.Dark -> Hct.fromInt(harmonizedSource)
				.also { it.tone = 10.0 }.toInt()
				.let { it to it.alpha(0.54f) }
		}
		
		return ItemColors(
			source = source,
			container = harmonizedSource,
			onContainer = onContainer,
			onContainerVariant = onContainerVariant,
			contentTone.correspondingPaletteTone
		)
	}
}

private object Contrast {
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

// Takes a color, a preferred tone and a contrast threshold and returns either Light or Dark
// ContentTone depending on if the preferred tone meets the contrast threshold or not
private fun calculateContentColorWithThreshold(
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

// Takes a color and returns either Light or Dark ContentTone depending on which scores better contrast
private fun calculateContentColorNeutral(color: Int) =
	listOf(
		ContentTone.Light to ColorUtils.calculateContrast(Color.WHITE, color),
		ContentTone.Dark  to ColorUtils.calculateContrast(Color.BLACK, color)
	)
	.maxBy { it.second }
	.first