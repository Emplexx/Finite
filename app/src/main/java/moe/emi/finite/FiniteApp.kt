package moe.emi.finite

import android.app.Application
import androidx.room.Room
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import dagger.hilt.android.HiltAndroidApp
import moe.emi.finite.service.db.FiniteDB

@HiltAndroidApp
class FiniteApp : Application() {
	
	companion object {
		
		lateinit var instance: FiniteApp
			private set
		
		lateinit var db: FiniteDB
			private set
		
		
	}
	
	override fun onCreate() {
		super.onCreate()
		instance = this
		
//		GlobalScope.launch {
//			when (appSettings.first().appTheme) {
//				AppTheme.Light -> launch(Dispatchers.Main) {
//					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//				}
//				AppTheme.Dark -> launch(Dispatchers.Main) {
//					AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//				}
//				else -> Unit
//			}
//			this.cancel()
//		}
		
//		GlobalScope.launch(Dispatchers.IO) {
//			appSettings.collect {
//				settingsSync = it
//			}
//		}
		
		initDb()
		
		theme.applyStyle(R.style.Theme_Finite, false)
		
		val dynamicColorsOptions = DynamicColorsOptions.Builder()
			.setThemeOverlay(R.style.ThemeOverlay_Finite_OutlineFix)
			.build()
		DynamicColors.applyToActivitiesIfAvailable(this, dynamicColorsOptions)
		
		val colors = arrayListOf(
			R.color.red,
			R.color.mint,
			R.color.pink,
		).toIntArray()
		
		val harmonyOptions = HarmonizedColorsOptions.Builder()
			.setColorResourceIds(colors)
			.build()
		
		HarmonizedColors.applyToContextIfAvailable(this, harmonyOptions)
	}
	
	private fun initDb() {
		
		db = Room
			.databaseBuilder(this, FiniteDB::class.java, "finite")
			.fallbackToDestructiveMigration()
			.build()
	}
}