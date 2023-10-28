package moe.emi.finite.dump

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.core.view.WindowInsetsControllerCompat


val Context.isDarkTheme: Boolean
	get() {
		val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
		return currentNightMode == Configuration.UI_MODE_NIGHT_YES
	}

var Activity.isStatusBarLightTheme: Boolean
	get() = WindowInsetsControllerCompat(window, window.decorView)
		.isAppearanceLightStatusBars
	set(value) {
		WindowInsetsControllerCompat(window, window.decorView)
			.isAppearanceLightStatusBars = value
	}

fun Activity.setStatusBarThemeMatchSystem() {
	isStatusBarLightTheme = !isDarkTheme
}