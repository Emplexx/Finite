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
import moe.emi.finite.components.details.deleteReminder
import moe.emi.finite.components.details.deleteSubscription
import moe.emi.finite.components.details.setSubscriptionActive
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.settings.store.colorOptions
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.NotificationDao
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.db.getBySubscriptionId
import moe.emi.finite.core.db.getSubscription
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.dump.Event
import moe.emi.finite.dump.with

class SubscriptionDetailsViewModel(
	savedState: SavedStateHandle,
	private val ratesRepo: RatesRepo,
	private val reminderScheduler: ReminderScheduler,
	private val settingsStore: SettingsStore,
	private val subscriptionDao: SubscriptionDao,
	private val reminderDao: NotificationDao,
) : ViewModel() {
	
	val entityId: Int = requireNotNull(savedState["ID"])
	
	val subscription = subscriptionDao
		.getSubscription(entityId)
		.combine(settingsStore.data, ::Pair)
		.combine(ratesRepo.fetchedRates) {
			(subscription, settings), rates ->
			subscription ?: return@combine null
			
			val convertedAmount = if (subscription.currency == settings.preferredCurrency) null
			else {
				rates?.convert(subscription.price, subscription.currency, settings.preferredCurrency)
			}
			
			SubscriptionDetailUiModel(subscription, settings.preferredCurrency, convertedAmount, settings.colorOptions)
		}
		.stateIn(viewModelScope, SharingStarted.Eagerly, null)
	
	val reminders = reminderDao.getBySubscriptionId(entityId)
	
	val events = MutableStateFlow<Event?>(null)
	
	
	fun togglePauseSubscription() = viewModelScope.launch {
		
		val isCurrentlyActive = subscription.value?.model?.isActive ?: return@launch
	
		with(subscriptionDao, reminderScheduler) {
			setSubscriptionActive(entityId, !isCurrentlyActive)
		}
			.onRight {
				events.update { Event(key = if (isCurrentlyActive) "Paused" else "Resumed") }
			}
			.onLeft {
				events.update { Event(Event.Error) }
			}
	}
	
	fun deleteSubscription() = viewModelScope.launch {
		with(subscriptionDao, reminderDao, reminderScheduler) {
			deleteSubscription(id = entityId)
		}
			.fold({ Event.Error }, { Event.Deleted })
			.let { events.value = Event(it) }
	}
	
	fun onDeleteReminder(id: Int) = viewModelScope.launch {
		with(reminderDao, reminderScheduler) { deleteReminder(id) }
	}
	
	companion object : VMFactory by singleViewModel({
		SubscriptionDetailsViewModel(
			ssh,
			container.ratesRepo,
			container.reminderScheduler,
			container.settingsStore,
			container.db.subscriptionDao(),
			container.db.notificationDao(),
		)
	})
	
}