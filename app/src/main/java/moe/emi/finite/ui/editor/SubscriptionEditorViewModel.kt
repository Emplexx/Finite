package moe.emi.finite.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.getDraft
import moe.emi.finite.service.db.SubscriptionEntity
import moe.emi.finite.service.model.Subscription

class SubscriptionEditorViewModel(
	savedState: SavedStateHandle,
) : ViewModel() {
	
	var subscription = savedState["Subscription"]
		?: runBlocking { getDraft().first() }
		?: Subscription(
			// TODO async?
			currency = runBlocking { FiniteApp.instance.appSettings.first().preferredCurrency }
		)
	private var subscriptionComparable = savedState["Subscription"]
		?: runBlocking { getDraft().first() }
		?: Subscription(
			// TODO async?
			currency = runBlocking { FiniteApp.instance.appSettings.first().preferredCurrency }
		)
		private set
	
	val hasUnsavedChanges: Boolean
		get() = subscription != subscriptionComparable
	
	fun saveSubscription() = viewModelScope.launch {
		FiniteApp.db.subscriptionDao()
			.insertAll(SubscriptionEntity(subscription))
			.also { moe.emi.finite.service.datastore.clearDraft() }
			.first()
			.also {
				subscription = subscription.copy(id = it.toInt())
				subscriptionComparable = subscription
			}
		
	}
	
	val canSave: Boolean
		get() = subscription.name.isNotBlank()
				&& subscription.startedOn != null
	
	suspend fun isDraft(): Boolean =
		subscription.id == 0
				&& subscription != Subscription(
					currency = FiniteApp.instance.appSettings.first().preferredCurrency
				)
	
	suspend fun clearDraft() {
		subscription = Subscription(
			currency = FiniteApp.instance.appSettings.first().preferredCurrency
		)
		subscriptionComparable = Subscription(
			currency = FiniteApp.instance.appSettings.first().preferredCurrency
		)
		moe.emi.finite.service.datastore.clearDraft()
	}
	
}