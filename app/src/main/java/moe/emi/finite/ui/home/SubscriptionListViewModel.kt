package moe.emi.finite.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.set
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.home.enums.TotalView
import javax.inject.Inject

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
	val savedState: SavedStateHandle
) : ViewModel() {
	
	val totalViewFlow = savedState.getStateFlow("Total", TotalView.Monthly)
	// TODO Save this in data store
	val totalViewLiveData = totalViewFlow.asLiveData()
	var totalView: TotalView
		get() = savedState["Total"] ?: TotalView.Monthly
		set(value) { savedState["Total"] = value }
	
	val settingsFlow = FiniteApp.instance.appSettings.asLiveData()
	var settings: AppSettings = AppSettings()
		private set
	
	var rates: List<Rate> = emptyList()
		private set
	
//	val showTimeLeftFlow = FiniteApp.instance.storeGeneral
//		.read(booleanPreferencesKey("ShowTimeLeft"), false)
//		.asLiveData()
	val showTimeLeftFlow = settingsFlow.map { it.showTimeLeft }
	
	
	private val _subscriptions by lazy { MutableLiveData<
			List<Subscription>
			>() }
	val subscriptions: LiveData<
			List<Subscription>
			> get() = _subscriptions
	
	
	
	
	val localRatesFlow = RatesRepo.getLocalRates()
	val filteredSubscriptionsFlow = SubscriptionsRepo.getAllSubscriptions()
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
	
	val totalSpentFlow =
		combine(localRatesFlow, filteredSubscriptionsFlow, totalViewFlow, FiniteApp.instance.appSettings) {
			rates, subscriptions, totalView, settings ->
			
			val sum = subscriptions
				.filter { it.active }
				.sumOf { subscription ->
					val rate = rates.find { it.code == subscription.currency.iso4217Alpha } ?: return@sumOf 0.0
					val prefererredRate = rates.find { it.code == settings.preferredCurrency.iso4217Alpha } ?: return@sumOf 0.0
					
					SubscriptionListFragment.convert(
						subscription.price,
						rate, prefererredRate,
						totalView,
						subscription.period
					).amountMatchedToTimeframe
				}
			
			Triple(totalView, sum, settings.preferredCurrency)
		}
	
	
	
	fun getSubscriptions() = viewModelScope.launch {
		
		combine(
			SubscriptionsRepo.getAllSubscriptions(),
			RatesRepo.getLocalRates(),
			FiniteApp.instance.appSettings,
		) { subscriptions, rates, appSettings ->
			
			if (rates.isEmpty()) { emptyList<Subscription>() }
			else {
				
				this@SubscriptionListViewModel.rates = rates
				this@SubscriptionListViewModel.settings = appSettings
				
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
			.collect {
				_subscriptions.postValue(it)
			}
	}
	
}