package moe.emi.finite.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.dump.invoke
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.service.model.Timespan
import moe.emi.finite.service.notifications.AlarmScheduler
import moe.emi.finite.service.repo.ReminderRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.util.Calendar

class ReminderEditorViewModel(
	private val savedState: SavedStateHandle,
	private val alarmScheduler: AlarmScheduler
) : ViewModel() {
	
	private val subscriptionId: Int = savedState["ID"]!!
	
	var reminder by savedState(Reminder.empty(subscriptionId))
	val reminderFlow = savedState.getStateFlow("reminder", reminder)
	
	var sameDay: Boolean by savedState(reminder.period == null)
	val sameDayFlow = savedState.getStateFlow("sameDay", sameDay)
	
	var period by savedState(reminder.period ?: NotificationPeriod(1, Timespan.Day))
	val periodFlow = savedState.getStateFlow("period", period)
	
	val parentSubscription = SubscriptionsRepo.getSubscription(subscriptionId).filterNotNull()
	
	val isPeriodValid = combine(parentSubscription, periodFlow) {
		subscription, period ->
		period < subscription.period
	}
	
	init {
		if (reminder.id == 0) {
			val (hour, minute) = Calendar.getInstance().let {
				it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
			}
			reminder = reminder.copy(hours = hour, minutes = minute)
		}
	}

	fun saveReminder(callback: () -> Unit) = viewModelScope.launch {
		
		val reminder = reminder.copy(
			period = if (sameDay) null else period
		)
		val result = ReminderRepo.insertAll(reminder)
		val isSuccess = result.size == 1
		
		if (isSuccess && parentSubscription.first().active) {
			alarmScheduler.scheduleAlarms(result.first().toInt())
			callback()
		}
	}
	
	companion object : VMFactory by singleViewModel({ ReminderEditorViewModel(ssh, container.alarmScheduler) })
}