package moe.emi.finite

import android.app.Application
import androidx.room.Room
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.db.FiniteDB
import kotlin.properties.Delegates

@HiltAndroidApp
class FiniteApp : Application() {
	
	companion object {
		
		lateinit var instance: FiniteApp
			private set
		
		lateinit var db: FiniteDB
			private set
		
		@OptIn(DelicateCoroutinesApi::class)
		var settings: AppSettings by Delegates.observable(AppSettings()) { prop, old, new ->
			GlobalScope.launch {
//				settings.
//				this.cancel()
			}
		}
	}
	
	override fun onCreate() {
		super.onCreate()
		instance = this
		
		initDb()
		
		theme.applyStyle(R.style.Theme_Finite, false)
		
		val dynamicColorsOptions = DynamicColorsOptions.Builder()
			.setThemeOverlay(R.style.ThemeOverlay_Finite_OutlineFix)
			.build()
		DynamicColors.applyToActivitiesIfAvailable(this, dynamicColorsOptions)
		
		val colors = arrayListOf(
			R.color.red,
			R.color.mint
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