package moe.emi.finite.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import moe.emi.finite.FiniteApp
import moe.emi.finite.components.editor.DraftStore
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.upgrade.billing.BillingConnection
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.FiniteDB
import moe.emi.finite.core.rates.RatesRepo

interface Container {
	
	val app: FiniteApp
	val scope: CoroutineScope
	val db: FiniteDB
	val reminderScheduler: ReminderScheduler
	
	val dataStore: DataStore<Preferences>
	val draftStore: DraftStore
	val settingsStore: SettingsStore
	
	val ratesRepo: RatesRepo
//	val reminderRepo: ReminderRepo
//	val subscriptionsRepo: SubscriptionsRepo
	
	val billingConnection: Flow<BillingConnection>
	val upgradeState: Flow<UpgradeState>
}