package moe.emi.finite.ui.colors

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.dump.getStorable
import moe.emi.finite.dump.isDarkTheme
import moe.emi.finite.service.datastore.storeGeneral

data class Tokens(
	val container: Int,
	val onContainer: Int,
)

val Context.colorTokens: Tokens
	get() {
		
		// between 0 - 10(?) where 0 would be the default container color (90 for light, 30 for dark)
		// and 10 is the amount that should be added/subtracted
		// eg 90 - 8 = 82 or 30 + 8 = 38
		// TODO async?
		val contrastFactor = runBlocking { storeGeneral.getStorable<AppSettings>().first().normalizeFactor }
		
		return when (isDarkTheme) {
			false -> Tokens(
				container = 90 - contrastFactor,
				onContainer = 10,
			)
			true -> Tokens(
				container = 30 + contrastFactor,
				onContainer = 90
			)
		}
	}

