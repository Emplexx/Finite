package moe.emi.finite.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.set
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.home.model.SubscriptionUiModel
import moe.emi.finite.ui.home.model.TotalView
import javax.inject.Inject

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
	val savedState: SavedStateHandle
) : ViewModel() {
	
	// TODO Save this in data store
	private val totalViewFlow = savedState.getStateFlow("Total", TotalView.Monthly)
	var totalView: TotalView
		get() = savedState["Total"] ?: TotalView.Monthly
		set(value) { savedState["Total"] = value }
	
	
	private val localRatesFlow = RatesRepo.getLocalRates()
	
	private val filteredSubscriptionsFlow = SubscriptionsRepo.getAllSubscriptions()
		.combine(FiniteApp.instance.appSettings) { subscriptions, appSettings ->
			subscriptions
				.filter {
					appSettings.selectedPaymentMethods.isEmpty()
							|| it.paymentMethod.trim().lowercase() in appSettings.selectedPaymentMethods
						.map { it.trim().lowercase() }
				}
				.also {
					// if the final returned list is empty and there are selected filters,
					// there is a chance that one of those selected filters was removed from
					// any item, so essentially it's broken and we clear filters manually
					if (it.isEmpty() && appSettings.selectedPaymentMethods.isNotEmpty()) {
						appSettings.copy(selectedPaymentMethods = emptySet()).set()
					}
				}
		}
	
	private val settingDefaultCurrency = FiniteApp.instance.appSettings.map { it.preferredCurrency }.distinctUntilChanged()
	
	val totalSpentFlow =
		combine(localRatesFlow, filteredSubscriptionsFlow, totalViewFlow, settingDefaultCurrency) {
			rates, subscriptions, totalView, defCurrency ->
			
			val sum = subscriptions
				.filter { it.active }
				.sumOf { subscription ->
					
					// TODO this has the same code as line 90ish
					val rate = rates.find { it.code == subscription.currency.iso4217Alpha } ?: return@sumOf 0.0
					val preferredRate = rates.find { it.code == defCurrency.iso4217Alpha } ?: return@sumOf 0.0
					
					SubscriptionListFragment.convert(
						subscription.price,
						rate, preferredRate,
						totalView,
						subscription.period
					).amountMatchedToTimeframe
				}
			
			Triple(totalView, sum, defCurrency)
		}
	
	val subscriptionsFlow =
		combine(
			filteredSubscriptionsFlow, localRatesFlow, settingDefaultCurrency, totalViewFlow
//			settingDefaultCurrency, settingShowTimeLeft
		) { subscriptions, rates, defCurrency, totalView -> //, defCurrency, showTimeLeft ->
			
			// TODO allow passing null converted amount to support unsupported currencies
			subscriptions.mapNotNull { subscription ->
				
				// TODO this has the same code as line 70ish
				val rate = rates.find { it.code == subscription.currency.iso4217Alpha }
				val preferredRate = rates.find { it.code == defCurrency.iso4217Alpha }
				val convertedAmount = rate?.let {
					preferredRate?.let {
						SubscriptionListFragment.convert(subscription.price, rate, preferredRate, totalView, subscription.period)
					}
				}
				
				convertedAmount?.let {
					subscription to it
				}
			}
		}
			.combine(FiniteApp.instance.appSettings) { list, settings ->
				list.map { (subscription, amount) ->
					SubscriptionUiModel(subscription, settings.preferredCurrency, amount, settings.showTimeLeft)
				}
			}
	
	
	
	fun getSubscriptions() = viewModelScope.launch {
		
		combine(
			SubscriptionsRepo.getAllSubscriptions(),
			RatesRepo.getLocalRates(),
			FiniteApp.instance.appSettings,
		) { subscriptions, rates, appSettings ->
			
			if (rates.isEmpty()) { emptyList<Subscription>() }
			else {
				
				
				subscriptions
					.filter {
						appSettings.selectedPaymentMethods.isEmpty()
								|| it.paymentMethod.trim().lowercase() in appSettings.selectedPaymentMethods
							.map { it.trim().lowercase() }
					}
					.also {
						// if the final returned list is empty and there are selected filters,
						// there is a chance that one of those selected filters was removed from
						// any item, so essentially it's broken and we clear filters manually
						if (it.isEmpty() && appSettings.selectedPaymentMethods.isNotEmpty()) {
							appSettings.copy(selectedPaymentMethods = emptySet()).set()
						}
					}
			}
		}
//			.zip(savedState.getStateFlow("Total", TotalView.Monthly)) { it, _ -> it }
//			.zip(savedState.getStateFlow("Sort", Sort.Date)) { it, _ -> it }
//			.collect {
//				_subscriptions.postValue(it)
//			}
	}
	
}