package moe.emi.finite.service.datastore

import kotlinx.coroutines.flow.StateFlow

class Settings(
	private val flow: StateFlow<AppSettings>
) : StateFlow<AppSettings> by flow {

//	private val flow = FiniteApp.instance.appSettings
//		.stateIn(FiniteApp.scope, SharingStarted.Eagerly, AppSettings())

}