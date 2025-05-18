package moe.emi.finite.di

import android.content.Context
import moe.emi.finite.FiniteApp

fun Context.memberInjection(
	action: Container.() -> Unit
) {
	(applicationContext as FiniteApp).container.action()
}