package moe.emi.finite.ui.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.emi.finite.service.data.Reminder
import moe.emi.finite.service.notifications.AlarmScheduler
import moe.emi.finite.service.repo.NotificationRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import javax.inject.Inject

@HiltViewModel
class SubscriptionDetailsViewModel @Inject constructor(
	savedState: SavedStateHandle,
	private val alarmScheduler: AlarmScheduler
) : ViewModel() {
	
	val entityId: Int = requireNotNull(savedState["ID"])
	val subscription = SubscriptionsRepo.getSubscription(entityId).asLiveData()
	val reminders = NotificationRepo.dao.getBySubscriptionId(entityId).map {
		it.map { Reminder(it) }
	}.asLiveData()

	val events = MutableLiveData<Event>(null)
	
	fun pauseSubscription() = viewModelScope.launch {
		SubscriptionsRepo.getSubscription(entityId).first()?.let { s ->
			SubscriptionsRepo.pauseSubscription(s.id, s.active)
			
			if (s.active) {
				alarmScheduler.removeAlarmsForSubscription(entityId)
			} else {
				alarmScheduler.scheduleAlarmsForSubscription(entityId)
			}
			
			events.postValue(
				Event(key = if (s.active) "Paused" else "Resumed")
			)
		}
	}
	
	fun deleteSubscription() = viewModelScope.launch {
		SubscriptionsRepo.deleteSubscription(entityId)
			.let { result ->
				if (result.isSuccess) {
					alarmScheduler.removeAlarmsForSubscription(entityId)
				}
				
				events.postValue(
					Event(if (result.isSuccess) Event.Delete else Event.Error)
				)
			}
	}
	
	fun deleteReminder(id: Int) = viewModelScope.launch {
		NotificationRepo.delete(id).also {
			if (it.isSuccess) alarmScheduler.removeAlarms(id)
		}
	}

//	companion object {
//		val Factory: ViewModelProvider.Factory = viewModelFactory {
//			initializer {
//				val ssh = createSavedStateHandle()
//				val myRepository = this[APPLICATION_KEY]
//			}
//		}
//	}
	
}