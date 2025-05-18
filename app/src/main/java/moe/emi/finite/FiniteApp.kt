package moe.emi.finite

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.DynamicColorsOptions
import com.google.android.material.color.HarmonizedColors
import com.google.android.material.color.HarmonizedColorsOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import moe.emi.finite.components.editor.newDraftStore
import moe.emi.finite.components.settings.store.newSettingsStore
import moe.emi.finite.components.upgrade.billing.createBillingConnection
import moe.emi.finite.components.upgrade.billing.processPurchasesForever
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.components.upgrade.cache.createUpgradeState
import moe.emi.finite.core.alarms.ReminderAlarmReceiver.Companion.REMINDER_CHANNEL_ID
import moe.emi.finite.core.alarms.newReminderScheduler
import moe.emi.finite.core.db.FiniteDB
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.core.rates.RemoteRates
import moe.emi.finite.di.Container

class FiniteApp : Application() {
	
	lateinit var container: Container
		private set
	
	companion object {
		
		lateinit var instance: FiniteApp
			private set
		
		val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
		
		private val Context.dataStore by preferencesDataStore("preferences")
	}
	
	override fun onCreate() {
		super.onCreate()
		
		instance = this
		
		container = object : Container {
			
			override val app = instance
			override val scope = FiniteApp.scope
			
			override val db = Room
				.databaseBuilder(app, FiniteDB::class.java, "finite")
				.setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
				.build()
			
			override val reminderScheduler = newReminderScheduler(app, db.notificationDao())
			
			override val dataStore = app.dataStore
			override val draftStore = newDraftStore(scope)
			override val settingsStore = newSettingsStore(scope)
			
			private val remoteRates = RemoteRates(app.dataStore)
			override val ratesRepo: RatesRepo = RatesRepo(remoteRates, app)
			
			override val billingConnection = createBillingConnection(app).shareIn(scope, SharingStarted.Eagerly, 1)
			override val upgradeState: Flow<UpgradeState> = createUpgradeState(app.dataStore, billingConnection, db.subscriptionDao())
		}
		
		scope.launch { processPurchasesForever(container.billingConnection, dataStore) }
		
		setupNotifications()
		setupMaterialYou()
	}
	
	@OptIn(DelicateCoroutinesApi::class)
	fun setupNotifications() {
		val manager = getSystemService<NotificationManager>()!!
		manager.createNotificationChannel(
			NotificationChannel(REMINDER_CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH)
		)
		GlobalScope.launch {
			container.reminderScheduler.invalidateAllReminders()
		}
	}
	
	private fun setupMaterialYou() {
		theme.applyStyle(R.style.Theme_Finite, false)
		
		val dynamicColorsOptions = DynamicColorsOptions.Builder()
			.setThemeOverlay(R.style.ThemeOverlay_Finite_OutlineFix)
			.build()
		DynamicColors.applyToActivitiesIfAvailable(this, dynamicColorsOptions)
		
		val colors = arrayListOf(
			R.color.red,
			R.color.mint,
			R.color.pink,
			R.color.blue
		).toIntArray()
		
		val harmonyOptions = HarmonizedColorsOptions.Builder()
			.setColorResourceIds(colors)
			.build()
		
		HarmonizedColors.applyToContextIfAvailable(this, harmonyOptions)
	}
}