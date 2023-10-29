package moe.emi.finite.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import moe.emi.finite.service.data.Reminder
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.data.Timespan
import moe.emi.finite.service.notifications.AlarmScheduler
import moe.emi.finite.service.repo.NotificationRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReminderEditorViewModel @Inject constructor(
	savedState: SavedStateHandle,
	private val alarmScheduler: AlarmScheduler
) : ViewModel() {
	
	private val entityId: Int = savedState["ID"]!!
	var reminder = savedState["Reminder"]
		?: Reminder(0, entityId, NotificationPeriod(1, Timespan.Day), 0, 0)
	
	var period = reminder.period ?: NotificationPeriod(1, Timespan.Day)
	
	private lateinit var subscription: Subscription
	
	init {
		if (reminder.id == 0) {
			val (hour, minute) = Calendar.getInstance().let {
				it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
			}
			reminder = reminder.copy(hours = hour, minutes = minute)
		}
		viewModelScope.launch {
			SubscriptionsRepo.getSubscription(entityId).collect {
				it?.let { subscription = it }
			}
		}
	}

	fun saveReminder(callback: () -> Unit) = viewModelScope.launch {
		NotificationRepo.insertAll(reminder).also {
			if (subscription.active && it.size == 1) alarmScheduler.scheduleAlarms(it.first().toInt())
		}
		callback()
	}
	
	val isPeriodValid: Boolean
		get() = period < subscription.period
}