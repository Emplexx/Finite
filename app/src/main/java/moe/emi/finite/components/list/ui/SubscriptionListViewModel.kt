package moe.emi.finite.components.list.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import moe.emi.finite.components.list.domain.TotalView
import moe.emi.finite.components.list.getFilteredSubscriptions
import moe.emi.finite.components.list.makeSubscriptionListUiModel
import moe.emi.finite.components.list.watchFilters
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.core.rates.RatesRepo
import moe.emi.finite.di.VMFactory
import moe.emi.finite.di.container
import moe.emi.finite.di.singleViewModel
import moe.emi.finite.di.ssh

class SubscriptionListViewModel(
	private val savedState: SavedStateHandle,
	private val settingsStore: SettingsStore,
	private val ratesRepo: RatesRepo,
	private val subscriptionDao: SubscriptionDao,
	val upgradeState: Flow<UpgradeState>,
) : ViewModel() {
	
	// TODO Save this in data store
	private val totalViewFlow = savedState.getStateFlow("Total", TotalView.Monthly)
	var totalView: TotalView
		get() = savedState["Total"] ?: TotalView.Monthly
		set(value) {
			savedState["Total"] = value
		}
	
	private val filteredSubscriptionsFlow =
		getFilteredSubscriptions(subscriptionDao, settingsStore, ratesRepo)
	
	val genderEquality = combine(
		filteredSubscriptionsFlow,
		ratesRepo.fetchedRates,
		totalViewFlow,
		settingsStore.data,
		::makeSubscriptionListUiModel
	)
	
	init {
		viewModelScope.launch { watchFilters(settingsStore, subscriptionDao) }
	}
	
	companion object : VMFactory by singleViewModel({
		SubscriptionListViewModel(
			ssh,
			container.settingsStore,
			container.ratesRepo,
			container.db.subscriptionDao(),
			container.upgradeState
		)
	})
	
}