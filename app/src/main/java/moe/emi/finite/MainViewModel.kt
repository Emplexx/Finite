package moe.emi.finite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import moe.emi.finite.di.NetworkModule
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.ui.details.Event
import moe.emi.finite.ui.settings.backup.Status
import javax.inject.Inject

class MainViewModel @Inject constructor(

) : ViewModel() {
	
	private val ratesApi = NetworkModule.getRatesApi()
	
	val messages = MutableLiveData<Event>(null)
	
	val ratesUpdateState = MutableStateFlow<Status?>(null)
	
	fun tryUpdateRates() = viewModelScope.launch {
//		RatesRepo.shouldRefreshRates().also { if (!it) return@launch }
		
		ratesUpdateState.emit(Status.Loading)
		
		RatesRepo.refreshRates()
			.ifSuccess { ratesUpdateState.emit(Status.Success) }
			.ifFailure { ratesUpdateState.emit(Status.Error) }
	}
	
}