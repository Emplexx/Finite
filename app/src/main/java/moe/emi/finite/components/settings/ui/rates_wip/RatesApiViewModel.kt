package moe.emi.finite.components.settings.ui.rates_wip

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import moe.emi.finite.dump.android.invoke
import moe.emi.finite.core.rates.api.ApiProvider

class RatesApiViewModel(
	val savedStateHandle: SavedStateHandle
) : ViewModel() {
	
	var provider by savedStateHandle(ApiProvider.InforEuro)
	val providerFlow = savedStateHandle.getStateFlow("provider", provider)
	
}