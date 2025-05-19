package moe.emi.finite.components.editor.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.emi.finite.components.editor.DraftStore
import moe.emi.finite.components.editor.saveSubscription
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.core.alarms.ReminderScheduler
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.model.Subscription
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh
import moe.emi.finite.dump.with

class SubscriptionEditorViewModel(
	private val savedState: SavedStateHandle,
	private val draft: DraftStore,
	private val settings: SettingsStore,
	private val scheduler: ReminderScheduler,
	private val dao: SubscriptionDao,
	private val appScope: CoroutineScope
) : ViewModel() {
	
	private val blankSubscription
		get() = Subscription(currency = settings.value.preferredCurrency)
	private val fallbackSubscription get() = draft.draft ?: blankSubscription
	
	init {
		if (!savedState.contains(KEY_SUBSCRIPTION))
			savedState[KEY_SUBSCRIPTION] = fallbackSubscription
		
		if (!savedState.contains(KEY_SUBSCRIPTION_STATIC)) {
			savedState.set<Subscription>(KEY_SUBSCRIPTION_STATIC, savedState[KEY_SUBSCRIPTION])
		}
	}
	
	private val subscriptionStatic1	get() = savedState.get<Subscription>(KEY_SUBSCRIPTION_STATIC)!!
	
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
	val subscriptionFlow = savedState.getStateFlow(KEY_SUBSCRIPTION, fallbackSubscription)
	
	val hasUnsavedChanges: Boolean
		get() = subscription != subscriptionStatic1
	
	val hasUnsavedChangesFlow = combine(
		savedState.getStateFlow(KEY_SUBSCRIPTION, subscription),
		savedState.getStateFlow(KEY_SUBSCRIPTION_STATIC, subscriptionStatic1)
	) { a, b -> a != b }
		.combine(subscriptionFlow.map { it.isDraft }, ::Pair)
	
	val showDraftIcon = subscriptionFlow.filterNotNull().map {
		it.id == 0 && it != blankSubscription
	}
	
	val canSave get() = subscription.name.isNotBlank() && subscription.startedOn != null
	
	val canSaveFlow = subscriptionFlow.filterNotNull().map {
		it.name.isNotBlank() && it.startedOn != null
	}
	
	fun onDiscardDraft() = viewModelScope.launch {
		blankSubscription.let {
			savedState[KEY_SUBSCRIPTION] = it
			savedState[KEY_SUBSCRIPTION_STATIC] = it
		}
		draft.clear()
	}
	
	fun onSaveSubscription() = viewModelScope.launch {
		with(dao, scheduler, draft) { saveSubscription(subscription) }
			.onRight {
				subscription = subscription.copy(id = it.toInt())
				savedState[KEY_SUBSCRIPTION_STATIC] = subscription
			}
	}
	
	fun onScreenClosed(appContext: Context) {
		if (subscription.isDraft && hasUnsavedChanges) appScope.launch {
			draft.updateData { subscription }
			withContext(Dispatchers.Main) {
				// TODO show this as a snackbar in main activity
				Toast.makeText(appContext, "Draft saved", Toast.LENGTH_SHORT).show()
			}
		}
	}
	
	companion object : VMFactory by singleViewModel({
		SubscriptionEditorViewModel(
			ssh,
			container.draftStore,
			container.settingsStore,
			container.reminderScheduler,
			container.db.subscriptionDao(),
			container.scope
		)
	}) {
		const val KEY_SUBSCRIPTION = "Subscription"
		const val KEY_SUBSCRIPTION_STATIC = "SubscriptionStatic"
	}
	
}