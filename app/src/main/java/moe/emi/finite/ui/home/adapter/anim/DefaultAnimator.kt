package moe.emi.finite.ui.home.adapter.anim

import android.animation.Animator
import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView

open class DefaultAnimator : BaseItemAnimator<ViewPropertyAnimator>() {
	
	// Add
	
	override fun addAnimationPrepare(holder: RecyclerView.ViewHolder) {
		holder.itemView.alpha = 0f
	}
	
	override fun addAnimation(holder: RecyclerView.ViewHolder, movesPending: Boolean): ViewPropertyAnimator {
		return holder.itemView.animate().alpha(1f).setDuration(addDuration)
	}
	
	override fun addAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.alpha = 1f
	}
	
	
	// Remove
	
	override fun removeAnimation(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
		return holder.itemView.animate().alpha(0f).setDuration(removeDuration)
	}
	
	override fun removeAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.alpha = 1f
	}
	
	
	// Change
	
	override fun changeOldAnimation(
		holder: RecyclerView.ViewHolder,
		changeInfo: ChangeInfo?
	): ViewPropertyAnimator {
		return holder.itemView.animate()
	}
	
	override fun changeNewAnimation(holder: RecyclerView.ViewHolder): ViewPropertyAnimator {
		return holder.itemView.animate()
	}
	
	override fun changeAnimationCleanup(holder: RecyclerView.ViewHolder?) {
	
	}
	
	
	// Move
	
	override fun animateMove(
		holder: RecyclerView.ViewHolder,
		x: Boolean,
		y: Boolean,
		additionsPending: Boolean
	): ViewPropertyAnimator {
		
		return holder.itemView.animate().apply {
			if (x) translationX(0f)
			if (y) translationY(0f)
			
			duration = moveDuration
		}
	}
	
	
	// Spring
	
	override fun setupAnimationListener(
		animation: ViewPropertyAnimator,
		onStart: () -> Unit,
		onEnd: () -> Unit,
		onCancel: () -> Unit
	) {
		animation.setListener(object : Animator.AnimatorListener {
			override fun onAnimationStart(animation: Animator) = onStart()
			
			override fun onAnimationEnd(animation: Animator) {
				onEnd()
			}
			
			override fun onAnimationCancel(animation: Animator) {
				onCancel()
			}
			
			override fun onAnimationRepeat(animation: Animator) {}
		})
	}
	
	override fun cleanupAnimationListener(animation: ViewPropertyAnimator) {
		animation.setListener(null)
	}
	
	override fun startAnimation(animation: ViewPropertyAnimator) {
		animation.start()
	}
}