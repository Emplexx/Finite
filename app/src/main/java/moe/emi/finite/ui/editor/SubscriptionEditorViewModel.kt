package moe.emi.finite.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.emi.finite.components.editor.DraftStore
import moe.emi.finite.components.editor.saveSubscription
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.dump.with
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.model.Subscription

class SubscriptionEditorViewModel(
	private val savedState: SavedStateHandle,
	private val draft: DraftStore,
	private val settings: SettingsStore,
	private val scheduler: ReminderScheduler,
	private val dao: SubscriptionDao,
) : ViewModel() {
	
	private val blankSubscription
		get() = Subscription(
			currency = settings.value.preferredCurrency
		)
	private val fallbackSubscription get() = draft.draft ?: blankSubscription
	
	private var subscriptionStatic = savedState[KEY_SUBSCRIPTION] ?: fallbackSubscription
	var subscription
		get() = savedState[KEY_SUBSCRIPTION] ?: fallbackSubscription
		set(value) {
			savedState[KEY_SUBSCRIPTION] = value
			if (value.id == 0) viewModelScope.launch {
				draft.updateData {
					value
				}
			}
		}
	
	init {
		if (!savedState.contains(KEY_SUBSCRIPTION)) savedState[KEY_SUBSCRIPTION] =
			fallbackSubscription
	}
	
	val subscriptionFlow = savedState.getStateFlow<Subscription?>(KEY_SUBSCRIPTION, null)
	val showDraftIcon = subscriptionFlow.filterNotNull().map {
		it.id == 0 && it != blankSubscription
	}
	val canSaveFlow = subscriptionFlow.filterNotNull().map {
		it.name.isNotBlank() && it.startedOn != null
	}
	
	fun discardDraft() = viewModelScope.launch {
		blankSubscription.let {
			savedState[KEY_SUBSCRIPTION] = it
			subscription = it
			subscriptionStatic = it
		}
		draft.clear()
	}
	
	val hasUnsavedChanges: Boolean
		get() = subscription != subscriptionStatic
	
	fun saveSubscription() = viewModelScope.launch {
		with(dao, scheduler, draft) { saveSubscription(subscription) }
			.onRight {
				subscription = subscription.copy(id = it.toInt())
				subscriptionStatic = subscription
			}
	}
	
	suspend fun saveDraft() {
		draft.updateData { subscription }
	}
	
	companion object : VMFactory by singleViewModel({
		SubscriptionEditorViewModel(
			ssh,
			container.draftStore,
			container.settingsStore,
			container.reminderScheduler,
			container.db.subscriptionDao()
		)
	}) {
		const val KEY_SUBSCRIPTION = "Subscription"
	}
	
}