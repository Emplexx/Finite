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
import moe.emi.finite.Constant
import moe.emi.finite.FiniteApp
import moe.emi.finite.dump.DataStoreExt.read
import moe.emi.finite.dump.Response
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.Keys
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.storeGeneral
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.home.enums.TotalView
import javax.inject.Inject

@HiltViewModel
class SubscriptionListViewModel @Inject constructor(
	val savedState: SavedStateHandle
) : ViewModel() {
	
	val totalViewFlow = savedState.getStateFlow("Total", TotalView.Monthly).asLiveData()
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
	
	
	fun getSubscriptions() = viewModelScope.launch {
		
		combine(
			SubscriptionsRepo.getSubscriptions(),
			RatesRepo.getLocalRates(),
			FiniteApp.instance.storeGeneral.read(Keys.RatesLastUpdated, 0L),
			FiniteApp.instance.appSettings,
		) { subscriptions, rates, lastUpdated, appSettings ->
			
			if (rates.isEmpty()) {
				// If local rates db is empty, refresh it quietly
				launch { RatesRepo. refreshRates() }
				emptyList<Subscription>()
			} else {
				
				// If local rates db is outdated, refresh it informing the user
				if (lastUpdated < System.currentTimeMillis() / 1000 - Constant.RatesUpdateInterval)
					updateRates()
				
				this@SubscriptionListViewModel.rates = rates
				this@SubscriptionListViewModel.settings = appSettings
				
				subscriptions.filter {
					appSettings.selectedPaymentMethods.isEmpty() || it.paymentMethod in appSettings.selectedPaymentMethods
				}
			}
		}
//			.zip(savedState.getStateFlow("Total", TotalView.Monthly)) { it, _ -> it }
//			.zip(savedState.getStateFlow("Sort", Sort.Date)) { it, _ -> it }
			.collect {
				_subscriptions.postValue(it)
			}
	}
	
	
	private val _ratesUpdateState by lazy { MutableLiveData<Response<Nothing?>>() }
	val ratesUpdateState: LiveData<Response<Nothing?>> get() = _ratesUpdateState
	
	private fun updateRates() = viewModelScope.launch {
		_ratesUpdateState.postValue(Response.Loading)
		RatesRepo.refreshRates().also { _ratesUpdateState.postValue(it) }
	}
	
	
	
}