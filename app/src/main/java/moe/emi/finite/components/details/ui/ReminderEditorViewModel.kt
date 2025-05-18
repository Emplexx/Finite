package moe.emi.finite.components.details.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.components.details.saveReminder
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.getSubscription
import moe.emi.finite.core.model.Period
import moe.emi.finite.core.model.Reminder
import moe.emi.finite.core.model.Timespan
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.dump.android.invoke
import moe.emi.finite.dump.with
import java.util.Calendar

class ReminderEditorViewModel(
	savedState: SavedStateHandle,
	private val reminderScheduler: ReminderScheduler,
	private val subscriptionDao: SubscriptionDao,
	private val reminderDao: NotificationDao,
) : ViewModel() {
	
	private val subscriptionId: Int = savedState["ID"]!!
	
	var reminder by savedState(Reminder.empty(subscriptionId))
	val reminderFlow = savedState.getStateFlow("reminder", reminder)
	
	var sameDay: Boolean by savedState(reminder.remindInAdvance == null)
	val sameDayFlow = savedState.getStateFlow("sameDay", sameDay)
	
	var period by savedState(reminder.remindInAdvance ?: Period(1, Timespan.Day))
	val periodFlow = savedState.getStateFlow("period", period)
	
	private val parentSubscription = subscriptionDao.getSubscription(subscriptionId).filterNotNull()
	
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
			remindInAdvance = if (sameDay) null else period
		)
		
		with(reminderScheduler, reminderDao) {
			saveReminder(reminder, parentSubscription.first().isActive)
		}
			.onRight { callback() }
	}
	
	companion object : VMFactory by singleViewModel({
		ReminderEditorViewModel(
			ssh,
			container.reminderScheduler,
			container.db.subscriptionDao(),
			container.db.notificationDao()
		)
	})
}