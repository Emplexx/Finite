package moe.emi.finite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.storedSettings
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.ui.details.Event
import moe.emi.finite.ui.settings.backup.Status

class MainViewModel : ViewModel() {
	
	val messages = MutableLiveData<Event>(null)
	
	val ratesUpdateState = MutableStateFlow<Status?>(null)
	
	fun tryUpdateRates() = viewModelScope.launch {
		RatesRepo.shouldRefreshRates().also { if (!it) return@launch }
		
		ratesUpdateState.emit(Status.Loading)
		
		RatesRepo.refreshRates()
			.onRight { ratesUpdateState.emit(Status.Success) }
			.onLeft { ratesUpdateState.emit(Status.Error) }
	}
	
	
	val selectedFilters = FiniteApp.instance.appSettings
		.map { it.selectedPaymentMethods }
		.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
	
	fun removeFilter(index: Int) = viewModelScope.launch {
		
		val element = selectedFilters.value.elementAtOrNull(index) ?: return@launch
		
		FiniteApp.instance.storedSettings.updateData {
			it.copy(
				selectedPaymentMethods = it.selectedPaymentMethods.minus(element)
			)
		}
	}
}