package moe.emi.finite.service.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp
import moe.emi.finite.R
import moe.emi.finite.service.data.Reminder
import moe.emi.finite.service.data.Subscription.Companion.findNextPaymentInclusive
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
	
	@Inject
	lateinit var alarmScheduler: AlarmScheduler
	
	override fun onReceive(context: Context?, intent: Intent?) {
		val id = intent?.getIntExtra("ID", -1)?.takeIf { it > -1 } ?: return
		context ?: return
		
		goAsync {
			
			val (reminder, model) = FiniteApp.db.notificationDao()
				.getById(id)
				.first()
				.firstOrNull()
				?.let { Reminder(it) to (SubscriptionsRepo.getSubscription(it.subscriptionId).first() ?: return@goAsync) }
				?: return@goAsync
			
			// TODO send notification
			Log.d("AlarmReceiver", "payment for ${model.name} is coming up soon!")
			
			val channelId = "Reminders"
			val notificationManager = context.getSystemService<NotificationManager>()!!
			val settings = context.appSettings.first()
			val convertedAmount = RatesRepo.fetchedRates.value
				?.convert(model.price, model.currency, settings.preferredCurrency)
			
			notificationManager.notify(id, NotificationCompat.Builder(context, channelId).apply {
				model.color?.let { color = it }
				
				setSmallIcon(R.drawable.ic_expand_more_24)
				setContentTitle(model.name.trim())
				
				setContentText(buildString {
					
					val (currency, price) = convertedAmount
						?.let { settings.preferredCurrency to it }
						?: (model.currency to model.price)
					
					append(currency.symbol ?: currency.iso4217Alpha)
					append(" ")
					// TODO format price decimal
					append(price)
					
					append(" due ")
					append(
						DateUtils.getRelativeTimeSpanString(
							model.startedOn!!.toLocalDate().findNextPaymentInclusive(model.period)
								.atTime(reminder.hours, reminder.minutes)
								.atZone(ZoneId.systemDefault())
								.toEpochSecond() * 1000L,
							
							System.currentTimeMillis(),
							DateUtils.DAY_IN_MILLIS
						).toString().lowercase()
					)
					
					append(" · ")
					append(DateTimeFormatter
						.ofLocalizedDate(FormatStyle.MEDIUM)
						.format(LocalDate.of(model.startedOn!!.year, model.startedOn.month, model.startedOn.day)))
					
				})
			}.build())
			
			alarmScheduler.scheduleAlarms(reminder.id)
		}
		
	}

	companion object {
		
		fun BroadcastReceiver.goAsync(
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