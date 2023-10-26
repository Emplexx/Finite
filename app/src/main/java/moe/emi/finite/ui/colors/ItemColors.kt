package moe.emi.finite.ui.colors

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.dump.getStorable
import moe.emi.finite.service.datastore.storeGeneral

data class ItemColors(
	val source: Int,
	val container: Int,
	val onContainer: Int,
)

@SuppressLint("RestrictedApi")
fun Context.makeItemColors(source: Int?): ItemColors {
	
	source ?: return ItemColors(
		materialColor(TonalColor.surfaceVariant),
		materialColor(TonalColor.surfaceVariant),
		materialColor(TonalColor.onSurface),
	)
	
	// TODO make function suspend?
	val settings = runBlocking { storeGeneral.getStorable<AppSettings>().first() }
	
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
		)
	} else {
		
		val hct = Hct.fromInt(harmonizedSource)
		val onContainer = if (hct.tone < 65) Color.WHITE
		else Hct.fromInt(harmonizedSource).also { it.tone = 10.0 }.toInt()
		
		return ItemColors(
			source = source,
			container = harmonizedSource,
			onContainer = onContainer,
		)
	}
}