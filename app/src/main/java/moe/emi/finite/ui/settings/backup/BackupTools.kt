package moe.emi.finite.ui.settings.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import moe.emi.finite.BuildConfig
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.db.NotificationEntity
import moe.emi.finite.service.db.SubscriptionEntity
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.service.model.Subscription

val jsonBackup = Json {
	ignoreUnknownKeys = true
	prettyPrint = true
}

suspend fun createAppBackup(): AppBackup {
	val subscriptions = FiniteApp.db.subscriptionDao()
		.getAllObservable()
		.first()
		.map { Subscription(it) }
		.map { SubscriptionBackup.from(it) }
	val reminders = FiniteApp.db.notificationDao()
		.getAll()
		.first()
		.map { Reminder(it) }
		.map { ReminderBackup.from(it) }
	
	return AppBackup(
		BuildConfig.VERSION_CODE,
		System.currentTimeMillis(),
		subscriptions,
		reminders
	)
}

suspend fun restoreAppBackup(backup: AppBackup) = runCatching {
	
	val subscriptionDao = FiniteApp.db.subscriptionDao()
	val reminderDao = FiniteApp.db.notificationDao()
	
	subscriptionDao.clearAll()
	reminderDao.clearAll()
	
	backup.subscriptions
		.map { Subscription.from(it) }
		.map { SubscriptionEntity(it) }
		.let { subscriptionDao.insertAll(*it.toTypedArray()) }
	
	backup.reminders
		.map { Reminder.from(it) }
		.map { NotificationEntity(it) }
		.let { reminderDao.insertAll(*it.toTypedArray()) }
	
	Unit
}


fun Context.writeBackupToFileV2(fileUri: Uri) = flow {
	emit(Status.Loading)
	val result = withContext(Dispatchers.IO) {
		runCatching {
			
			val input = createAppBackup().serialize().byteInputStream()
			val output = contentResolver.openOutputStream(fileUri) ?: error("Content resolver crashed.")
			
			input.use { i ->
				output.use { o ->
					i.copyTo(o)
				}
			}
		}
	}
	result
		.onFailure {
			it.printStackTrace()
			emit(Status.Error)
		}
		.onSuccess {
			emit(Status.Success)
		}
}


fun Context.readDbFromFileV2(fileUri: Uri) = flow {
	emit(Status.Loading)
	
	val result = withContext(Dispatchers.IO) {
		runCatching {
			val input = contentResolver.openInputStream(fileUri) ?: error("Content resolver crashed.")
//			val reader = BufferedReader(input.bufferedReader())
//				.use {  }
//			val s = StringBuilder()
			
			val string = input.use { it.bufferedReader().use { reader -> reader.readText() } }
			val backup = jsonBackup.decodeFromString(AppBackup.serializer(), string)
			
			restoreAppBackup(backup).getOrThrow()
		}
	}
	
	result
		.onFailure { it.printStackTrace(); emit(Status.Error) }
		.onSuccess { emit(Status.Success) }
}