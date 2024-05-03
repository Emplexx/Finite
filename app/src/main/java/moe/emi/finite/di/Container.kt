package moe.emi.finite.di

import android.content.Context
import moe.emi.finite.FiniteApp
import moe.emi.finite.service.notifications.AlarmScheduler

interface Container {
	
	val app: FiniteApp
	
	val alarmScheduler: AlarmScheduler
	
}

val Context.app
	get() = (applicationContext as FiniteApp)

val container
	get() = FiniteApp.instance.container