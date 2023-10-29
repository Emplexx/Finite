package moe.emi.finite

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import moe.emi.finite.service.db.FiniteDB
import moe.emi.finite.service.notifications.AlarmScheduler
import javax.inject.Inject

@HiltAndroidApp
class FiniteApp : Application() {
	
	@Inject
	lateinit var alarmScheduler: AlarmScheduler
	
	companion object {
		
		lateinit var instance: FiniteApp
			private set
		
		private var dbInstance: FiniteDB? = null
		val db: FiniteDB
			get() = dbInstance!!
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
		setupNotifications()
		
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
	
	fun initDb() {
		
		dbInstance = Room
			.databaseBuilder(this, FiniteDB::class.java, "finite")
			.fallbackToDestructiveMigration()
			.setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
			.build()
	}
	
	// TODO does't fix the issue of having to restart the app so fix it
	fun destroyDb() { dbInstance = null }
	
	@OptIn(DelicateCoroutinesApi::class)
	fun setupNotifications() {
		val manager = getSystemService<NotificationManager>()!!
		manager.createNotificationChannel(
			NotificationChannel("Reminders", "Reminders", NotificationManager.IMPORTANCE_HIGH)
		)
		GlobalScope.launch {
			alarmScheduler.invalidateAllAlarms()
		}
	}
	
}