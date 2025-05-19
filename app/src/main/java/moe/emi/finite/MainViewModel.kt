package moe.emi.finite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.dump.Event

class MainViewModel(
	private val ratesRepo: RatesRepo,
	val settingsStore: SettingsStore,
	upgradeState: Flow<UpgradeState>,
	subscriptionDao: SubscriptionDao,
) : ViewModel() {
	
	val messages = MutableLiveData<Event>(null)
	val ratesUpdateState = MutableStateFlow<Status?>(null)
	
	val isPro = upgradeState.map { it.isPro }
		.shareIn(viewModelScope, SharingStarted.Eagerly, 1)
	val subscriptionCount = subscriptionDao.getAllObservable().map { it.size }
	
	fun tryUpdateRates() = viewModelScope.launch {
		ratesRepo.shouldRefreshRates().also { if (!it) return@launch }
		
		ratesUpdateState.emit(Status.Loading)
		
		ratesRepo.refreshRates()
			.onRight { ratesUpdateState.emit(Status.Success) }
			.onLeft { ratesUpdateState.emit(Status.Error) }
	}
	
	val selectedFilters = settingsStore.data
		.map { it.selectedPaymentMethods }
		.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
	
	fun removeFilter(index: Int) = viewModelScope.launch {
		
		val element = selectedFilters.value.elementAtOrNull(index) ?: return@launch
		
		settingsStore.updateData {
			it.copy(
				selectedPaymentMethods = it.selectedPaymentMethods.minus(element)
			)
		}
	}
	
	companion object : VMFactory by singleViewModel({
		MainViewModel(container.ratesRepo, container.settingsStore, container.upgradeState, container.db.subscriptionDao())
	})
}