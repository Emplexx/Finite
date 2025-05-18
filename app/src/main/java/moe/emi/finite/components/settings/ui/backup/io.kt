package moe.emi.finite.components.settings.ui.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import moe.emi.finite.BuildConfig
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.FiniteDB
import moe.emi.finite.core.db.SubscriptionEntity
import moe.emi.finite.core.db.toEntity
import moe.emi.finite.core.db.toReminder
import moe.emi.finite.core.db.toSubscription

val jsonBackup = Json {
	ignoreUnknownKeys = true
	prettyPrint = true
}

context(FiniteDB)
suspend fun createAppBackup(): AppBackup = withContext(Dispatchers.IO) {
	
	val subscriptions = subscriptionDao()
		.getAllObservable()
		.first()
		.map { it.toSubscription() }
		.map { SubscriptionBackup.from(it) }
	
	val reminders = notificationDao()
		.getAll()
		.first()
		.map { it.toReminder().toBackup() }
	
	// TODO include settings
	
	AppBackup(
		BuildConfig.VERSION_CODE,
		System.currentTimeMillis(),
		subscriptions,
		reminders
	)
}

// TODO implement replace existing = false
context(FiniteDB, ReminderScheduler)
suspend fun restoreAppBackup(backup: AppBackup, replaceExisting: Boolean) = kotlin.runCatching {
	withContext(Dispatchers.IO) {
		cancelAllReminders()
		
		val subscriptionDao = subscriptionDao()
		val reminderDao = notificationDao()
		
		subscriptionDao.clearAll()
		reminderDao.clearAll()
		
		backup.subscriptions
			.map { it.toSubscription() }
			.map { SubscriptionEntity(it) }
			.let { subscriptionDao.insertAll(*it.toTypedArray()) }
		
		backup.reminders
			.map { it.toReminder().toEntity() }
			.let { reminderDao.insertAll(*it.toTypedArray()) }
		
		invalidateAllReminders()
	}
	Unit
}

context(Context)
suspend fun writeBackupFile(
	fileUri: Uri,
	backup: AppBackup
) = withContext(Dispatchers.IO) {
	kotlin.runCatching {
		
		val input = backup.serialize().byteInputStream()
		val output = contentResolver.openOutputStream(fileUri) ?: error("Content resolver crashed.")
		
		input.use { i ->
			output.use { o ->
				i.copyTo(o)
			}
		}
	}
}

context(Context)
suspend fun readBackupFile(
	fileUri: Uri
) = withContext(Dispatchers.IO) {
	kotlin.runCatching {
		
		val input = contentResolver.openInputStream(fileUri) ?: error("Content resolver crashed.")
		val string = input.use { it.bufferedReader().use { reader -> reader.readText() } }
		jsonBackup.decodeFromString(AppBackup.serializer(), string)
	}
}