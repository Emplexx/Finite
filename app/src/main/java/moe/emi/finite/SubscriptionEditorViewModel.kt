package moe.emi.finite

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.db.SubscriptionEntity
import javax.inject.Inject

@HiltViewModel
class SubscriptionEditorViewModel @Inject constructor(
	val savedState: SavedStateHandle,
) : ViewModel() {
	
	var subscription = savedState["Subscription"]
		?: Subscription(
			currency = Currency.EUR // TODO get default currency here
		)
	
	fun saveSubscription() = viewModelScope.launch {
		FiniteApp.db.subscriptionDao().insertAll(SubscriptionEntity(subscription))
	}
	
}