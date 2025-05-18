package moe.emi.finite.dump

import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.DisplayMetrics
import android.view.Window

data class Display(val width : Int, val height : Int)

fun requestDisplayDimensions(window: Window): Display {
	
	return if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
		val bounds = window
			.windowManager
			.currentWindowMetrics
			.bounds
		
		Display(bounds.width(), bounds.height())
	} else {
		val displayMetrics = DisplayMetrics()
		window.windowManager.defaultDisplay.getMetrics(displayMetrics)
		
		Display(displayMetrics.widthPixels, displayMetrics.heightPixels)
	}
	
}