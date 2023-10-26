package moe.emi.finite.dump.deco

import android.content.Context
import moe.emi.aluminium.features.settings.core.deco.AutoLayoutDecor
import moe.emi.aluminium.features.settings.core.deco.Deco
import moe.emi.aluminium.features.settings.core.deco.DividerDeco
import moe.emi.aluminium.features.settings.core.deco.RoundDecor
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.iDp

fun Context.tableViewDecor() = Deco.Builder()
	.underlay(RoundDecor(8.fDp))
	.overlay(DividerDeco(this, 16.iDp, 16.iDp))
	.offset(AutoLayoutDecor(16.iDp))
	.build()