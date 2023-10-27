package moe.emi.finite

import moe.emi.finite.dump.setStorable
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.storeGeneral

//var settings: AppSettings by Delegates.observable(AppSettings()) { _, old, new ->
//
//	if (old != new) GlobalScope.launch {
//		FiniteApp.instance.storeGeneral.setStorable(new)
//		this.cancel()
//	}
//}

//var settings: AppSettings
//	get() = runBlocking { FiniteApp.instance.storeGeneral.getStorable<AppSettings>().first() }
//	set(value) {
//		GlobalScope.launch {
//			FiniteApp.instance.storeGeneral.setStorable(value)
//			this.cancel()
//		}
//	}


suspend fun AppSettings.set() {
	FiniteApp.instance.storeGeneral.setStorable(this)
}