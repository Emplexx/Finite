package moe.emi.finite.core.alarms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.R
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.core.db.FiniteDB
import moe.emi.finite.core.db.getSubscription
import moe.emi.finite.core.db.toReminder
import moe.emi.finite.core.findNextPaymentInclusive
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Reminder
import moe.emi.finite.core.model.Subscription
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.di.memberInjection
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class ReminderAlarmReceiver : BroadcastReceiver() {
	
	private lateinit var reminderScheduler: ReminderScheduler
	private lateinit var db: FiniteDB
	private lateinit var ratesRepo: RatesRepo
	private lateinit var upgradeState: Flow<UpgradeState>
	private lateinit var settingsStore: SettingsStore
	
	override fun onReceive(context: Context?, intent: Intent?) {
		
		val reminderId = intent?.getIntExtra("ID", -1)?.takeIf { it > -1 } ?: return
		
		(context ?: return).memberInjection {
			reminderScheduler = it.reminderScheduler
			db = it.db
			ratesRepo = it.ratesRepo
			upgradeState = it.upgradeState
			settingsStore = it.settingsStore
		}
		
		goAsync { context.doTask(reminderId) }
	}

	private suspend fun Context.doTask(reminderId: Int) {
		
		if (upgradeState.first().isIllegalPro) return
		
		val reminder = db.notificationDao().getById(reminderId).first().firstOrNull()?.toReminder() ?: return
		val model = db.subscriptionDao().getSubscription(reminder.subscriptionId).first() ?: return
		
		val settings = settingsStore.data.first()
		val due = ratesRepo.fetchedRates.value
			?.convert(model.price, model.currency, settings.preferredCurrency)
			?.let { convertedPrice -> settings.preferredCurrency to convertedPrice }
			?: (model.currency to model.price)
		
		showReminderNotification(reminder, model, due)
		
		reminderScheduler.scheduleReminders(reminderId)
	}
	
	private fun Context.showReminderNotification(
		reminder: Reminder,
		subscription: Subscription,
		due: Pair<Currency, Double>
	) {
		NotificationCompat.Builder(this@Context, REMINDER_CHANNEL_ID).apply {
			if (subscription.color != null) color = subscription.color
			// TODO icon
			setSmallIcon(R.drawable.ic_expand_more_24)
			setContentTitle(subscription.name.trim())
			
			setContentText(buildString {
				
				val (currency, price) = due
				
				append(currency.symbol ?: currency.iso4217Alpha)
				append(" ")
				// TODO format price decimal
				append(price)
				
				append(" due ")
				append(
					DateUtils.getRelativeTimeSpanString(
						subscription.startedOn!!.toLocalDate().findNextPaymentInclusive(subscription.period)
							.atTime(reminder.hours, reminder.minutes)
							.atZone(ZoneId.systemDefault())
							.toEpochSecond() * 1000L,
						
						System.currentTimeMillis(),
						DateUtils.DAY_IN_MILLIS
					).toString().lowercase()
				)
				
				append(" · ")
				append(
					DateTimeFormatter
						.ofLocalizedDate(FormatStyle.MEDIUM)
						.format(LocalDate.of(subscription.startedOn.year, subscription.startedOn.month, subscription.startedOn.day))
				)
			})
		}
			.build()
			.let { getSystemService<NotificationManager>()?.notify(reminder.id, it) }
	}
	
	companion object {
		
		const val REMINDER_CHANNEL_ID = "Reminders"
		
		private fun BroadcastReceiver.goAsync(
			context: CoroutineContext = EmptyCoroutineContext,
			block: suspend CoroutineScope.() -> Unit
		) {
			val pendingResult = goAsync()
			@OptIn(DelicateCoroutinesApi::class) // Must run globally; there's no teardown callback.
			GlobalScope.launch(context) {
				try { block() }
				finally { pendingResult.finish() }
			}
		}
		
	}
}