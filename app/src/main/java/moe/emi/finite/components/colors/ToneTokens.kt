package moe.emi.finite.components.colors

import android.content.Context
import moe.emi.finite.dump.isDarkTheme

data class ToneTokens(
	val container: Int,
	val onContainer: Int,
)

fun Context.normalizedToneTokens(contrastFactor: Int): ToneTokens {
	
	// between 0 - 10(?) where 0 would be the default container color (90 for light, 30 for dark)
	// and 10 is the amount that should be added/subtracted
	// eg 90 - 8 = 82 or 30 + 8 = 38
	
	return when (isDarkTheme) {
		false -> ToneTokens(
			container = 90 - contrastFactor,
			onContainer = 10,
		)
		true -> ToneTokens(
			container = 30 + contrastFactor,
			onContainer = 90
		)
	}
}

