package moe.emi.finite.components.colors

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import com.google.android.material.color.utilities.Blend
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.TonalPalette
import convenience.resources.Token
import convenience.resources.colorAttr
import moe.emi.finite.components.settings.store.ColorOptions
import moe.emi.finite.dump.alpha

data class ItemColors(
	val source: Int,
	val container: Int,
	val onContainer: Int,
	val onContainerVariant: Int,
	val tone: PaletteTone,
) {
	constructor(context: Context) : this(
		source = context.colorAttr(Token.color.surfaceVariant),
		container = context.colorAttr(Token.color.surfaceVariant),
		onContainer = context.colorAttr(Token.color.onSurface),
		onContainerVariant = context.colorAttr(Token.color.onSurface).alpha(0.54f),
		tone = PaletteTone.DeviceTheme
	)
}

@SuppressLint("RestrictedApi")
fun Context.makeItemColors(
	source: Int?,
	options: ColorOptions
): ItemColors {
	source ?: return ItemColors(this)

	val (harmonize, normalize) = options
	
	val harmonizedSource =
		if (!harmonize) source
		else Blend.harmonize(source, colorAttr(Token.color.primary))
	
	if (normalize) {
		val tokens = normalizedToneTokens(options.normalizeFactor)
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

		val contentTone =
//			calculateContentColorNeutral(hct.toInt())
			calculateContentColorWithThreshold(
				hct.toInt(),
				ContentTone.Light, Contrast.LargeAA
			)
		
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