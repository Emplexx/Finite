package moe.emi.finite.dump

import android.animation.ValueAnimator
import android.view.View
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

fun interface LiftOnScrollListener {
	fun callback(shouldLift: Boolean)
}

class AlphaOnLiftListener(
	val view: View
) : LiftOnScrollListener {
	
	override fun callback(shouldLift: Boolean) {
		if (shouldLift) elevateUp()
		else elevateDown()
	}
	
	
	var upAnimator: ValueAnimator? = null
	var downAnimator: ValueAnimator? = null
	
	private fun elevateUp() {
		
		if (upAnimator == null) upAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
			duration = 200
			interpolator = FastOutSlowInInterpolator()
			addUpdateListener {
				view.alpha = it.animatedValue as Float
			}
		}
		
		upAnimator?.start()
		downAnimator?.cancel()
	}
	
	private fun elevateDown() {
		
		if (downAnimator == null) downAnimator = ValueAnimator.ofFloat(1f, 0f).apply {
			duration = 1200
			interpolator = FastOutExtraSlowInInterpolator()
			addUpdateListener {
				view.alpha = it.animatedValue as Float
			}
		}
		
		downAnimator?.start()
		upAnimator?.cancel()
	}
}