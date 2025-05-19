package moe.emi.finite.di

import android.content.Context
import moe.emi.finite.FiniteApp

fun Context.memberInjection(
	inject: (Container) -> Unit
) {
	inject((applicationContext as FiniteApp).container)
}