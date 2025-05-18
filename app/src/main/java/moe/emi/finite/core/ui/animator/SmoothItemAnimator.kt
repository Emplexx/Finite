package moe.emi.finite.core.ui.animator

import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView
import convenience.resources.Token
import convenience.resources.easingAttr

open class SmoothItemAnimator(
	val scaleInit: Float = 0.92f,
	val scaleToRemove: Float = 0.96f
) : DefaultAnimator() {
	
	init {
		moveDuration = 450
		addDuration = 550
	}
	
//	companion object {
//		const val scaleInit = 0.92f
//		const val scaleToRemove = 0.96f
//	}
	
	override fun addAnimationPrepare(holder: RecyclerView.ViewHolder) {
		holder.itemView.apply {
			scaleX = scaleInit
			scaleY = scaleInit
			alpha = 0f
		}
	}
	override fun addAnimation(holder: RecyclerView.ViewHolder, movesPending: Boolean): ViewPropertyAnimator {
		return holder.itemView.animate()
			.scaleX(1f)
			.scaleY(1f)
			.alpha(1f)
			.apply {
				if (movesPending) this
//					.setDuration(250)
					.setDuration(addDuration)
					.setInterpolator(
						holder.itemView.context.easingAttr(Token.easing.emphasizedDecelerated)
					)
				else this
					.setDuration(addDuration)
					.setInterpolator(
						holder.itemView.context.easingAttr(Token.easing.emphasized)
					)
			}
	}
	override fun addAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.apply {
			scaleX = 1f
			scaleY = 1f
			alpha = 1f
		}
	}
	
	override fun removeAnimation(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
		return holder.itemView.animate()
			.scaleX(scaleToRemove)
			.scaleY(scaleToRemove)
			.alpha(0f)
			.setDuration(removeDuration)
			.setInterpolator(
				holder.itemView.context.easingAttr(Token.easing.emphasizedAccelerated)
			)
	}
	
	override fun removeAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.apply {
			scaleX = 1f
			scaleY = 1f
			alpha = 1f
		}
	}
	
	override fun getAddDelay(remove: Long, move: Long, change: Long): Long = 100L
	
	
	override fun animateMove(
		holder: RecyclerView.ViewHolder,
		x: Boolean,
		y: Boolean,
		additionsPending: Boolean
	): ViewPropertyAnimator {
		return holder.itemView.animate().apply {
			if (x) translationX(0f)
			if (y) translationY(0f)
			
//			interpolator = if (additionsPending)
//				holder.itemView.context.easingAttr(Token.easing.emphasized)
//			else holder.itemView.context.easingAttr(Token.easing.emphasizedDecelerated)
//			duration = if (additionsPending) 200 else moveDuration
			
			interpolator = holder.itemView.context.easingAttr(Token.easing.emphasizedDecelerated)
			duration = moveDuration
		}
	}
	
	override fun cleanupMove(holder: RecyclerView.ViewHolder, x: Boolean, y: Boolean) {
		holder.itemView.apply {
			if (x) translationX = 0f
			if (y) translationY = 0f
		}
	}
	
	override fun changeAnimationCleanup(holder: RecyclerView.ViewHolder?) {
		super.changeAnimationCleanup(holder)
	}
	
	override fun cleanupAnimationListener(animation: ViewPropertyAnimator) {
		super.cleanupAnimationListener(animation)
	}
	
}