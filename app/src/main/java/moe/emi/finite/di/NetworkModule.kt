package moe.emi.finite.di

import moe.emi.finite.service.api.ApiProvider
import moe.emi.finite.service.api.impl.InforEuro

object NetworkModule {
	
	private val def by lazy { InforEuro() }
	fun getRatesApi(): ApiProvider.Impl = def
}