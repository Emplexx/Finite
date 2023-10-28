package moe.emi.finite.ui.details

import android.content.res.ColorStateList
import androidx.core.view.forEach
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import moe.emi.finite.dump.colorAnimator

abstract class AppBarChangeColorListener(
	private val toolbar: MaterialToolbar
) : AppBarStateChangeListener() {
	
	abstract fun getExpandedColor(): Int
	abstract fun getCollapsedColor(): Int
	
	open fun onExpandCallback() = Unit
	open fun onCollapseCallback() = Unit
	
	var lastAnimatedState = State.EXPANDED
		private set
	private var _lastColor: Int? = null
	private var lastColor: Int
		get() = _lastColor ?: getExpandedColor()
		set(value) { _lastColor = value }
	
	private fun applyColor(color: Int) {
		toolbar.setNavigationIconTint(color)
		toolbar.menu.forEach {
			it.iconTintList = ColorStateList.valueOf(color)
		}
		toolbar.overflowIcon?.setTint(color)
	}
	
	override fun onStateChanged(appBarLayout: AppBarLayout?, state: State?) {
		state ?: return
		
		when (state) {
			State.EXPANDED -> {
				if (lastAnimatedState == State.EXPANDED) return
				colorAnimator(lastColor to getExpandedColor(), 100) {
					val v = it.animatedValue as Int
					applyColor(v)
				}.start()
				lastColor = getExpandedColor()
				lastAnimatedState = State.EXPANDED
				onExpandCallback()
			}
			State.COLLAPSED -> {
				if (lastAnimatedState == State.COLLAPSED) return
				colorAnimator(lastColor to getCollapsedColor(), 100) {
					val v = it.animatedValue as Int
					applyColor(v)
				}.start()
				lastColor = getCollapsedColor()
				lastAnimatedState = State.COLLAPSED
				onCollapseCallback()
			}
			State.IDLE -> Unit
		}
	}
	
}