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
import moe.emi.finite.service.datastore.appSettings

data class ItemColors(
	val source: Int,
	val container: Int,
	val onContainer: Int,
	val onContainerVariant: Int,
)

@SuppressLint("RestrictedApi")
fun Context.makeItemColors(source: Int?): ItemColors {
	
	source ?: return ItemColors(
		source = materialColor(TonalColor.surfaceVariant),
		container = materialColor(TonalColor.surfaceVariant),
		onContainer = materialColor(TonalColor.onSurface),
		onContainerVariant = materialColor(TonalColor.onSurface).alpha(0.54f)
	)
	
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
		)
	} else {
		
		val hct = Hct.fromInt(harmonizedSource)
//		val onContainer = if (hct.tone < 65) Color.WHITE
//		else Hct.fromInt(harmonizedSource).also { it.tone = 10.0 }.toInt()
		
		val (onContainer, onContainerVariant) =
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
		
		return ItemColors(
			source = source,
			container = harmonizedSource,
			onContainer = onContainer,
			onContainerVariant = onContainerVariant
		)
	}
}

private object Contrast {
	const val NormalAAA = 7.0
	const val NormalAA = 4.5
	const val LargeAAA = 4.5
	const val LargeAA = 3.0
}

// Takes a color and returns either Color.WHITE or Color.BLACK depending on which scores better contrast
private fun calculateForegroundColor(color: Int) =
	listOf(
		Color.WHITE to ColorUtils.calculateContrast(Color.WHITE, color),
		Color.BLACK to ColorUtils.calculateContrast(Color.BLACK, color)
	).maxBy { it.second }.first