package moe.emi.finite.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.notifications.AlarmScheduler
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.service.repo.ReminderRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.details.model.SubscriptionDetailUiModel

class SubscriptionDetailsViewModel(
	savedState: SavedStateHandle,
	private val alarmScheduler: AlarmScheduler
) : ViewModel() {
	
	val entityId: Int = requireNotNull(savedState["ID"])
	
	val subscription = SubscriptionsRepo
		.getSubscription(entityId)
		.combine(FiniteApp.instance.appSettings) { a, b -> a to b }
		.combine(RatesRepo.fetchedRates) {
			(subscription, settings), rates ->
			subscription ?: return@combine null
			
			val convertedAmount = if (subscription.currency == settings.preferredCurrency) null
			else {
				rates?.convert(subscription.price, subscription.currency, settings.preferredCurrency)
			}
			
			SubscriptionDetailUiModel(subscription, settings.preferredCurrency, convertedAmount)
		}
		.stateIn(viewModelScope, SharingStarted.Eagerly, null)
	
	val reminders = ReminderRepo.getBySubscriptionId(entityId)
	
	val events = MutableStateFlow<Event?>(null)
	
	
	fun pauseSubscription() = viewModelScope.launch {
		
		val subscription = subscription.value?.model ?: return@launch
		
		SubscriptionsRepo.pauseSubscription(subscription.id, subscription.active)
		
		if (subscription.active)
			alarmScheduler.removeAlarmsForSubscription(entityId)
		else
			alarmScheduler.scheduleAlarmsForSubscription(entityId)
		
		events.update { Event(key = if (subscription.active) "Paused" else "Resumed") }
	}
	
	fun deleteSubscription() = viewModelScope.launch {
		
		val result = SubscriptionsRepo.deleteSubscription(entityId)
			.onSuccess {
				alarmScheduler.removeAlarmsForSubscription(entityId)
				ReminderRepo.dao.deleteBySubscriptionId(entityId)
			}
		
		events.update { Event(if (result.isSuccess) Event.Deleted else Event.Error) }
	}
	
	fun deleteReminder(id: Int) = viewModelScope.launch {
		ReminderRepo.delete(id)
			.onSuccess { alarmScheduler.removeAlarms(id) }
	}

//	companion object {
//		val Factory: ViewModelProvider.Factory = viewModelFactory {
//			initializer {
//				val ssh = createSavedStateHandle()
//				val myRepository = this[APPLICATION_KEY]
//			}
//		}
//	}
	
	companion object : VMFactory by singleViewModel({
		SubscriptionDetailsViewModel(ssh, container.alarmScheduler)
	})
	
}