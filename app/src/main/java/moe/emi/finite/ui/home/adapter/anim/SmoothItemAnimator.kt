package moe.emi.finite.ui.home.adapter.anim

import android.view.ViewPropertyAnimator
import androidx.recyclerview.widget.RecyclerView
import moe.emi.convenience.Interpolator
import moe.emi.convenience.materialInterpolator

class SmoothItemAnimator : DefaultAnimator() {
	
	init {
		moveDuration = 450
		addDuration = 550
	}
	
	companion object {
		const val scaleInit = 0.92f
		const val scaleToRemove = 0.96f
	}
	
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
					.setDuration(250)
					.setInterpolator(
						holder.itemView.context.materialInterpolator(Interpolator.emphasizedDecelerated)
					)
				else this
					.setDuration(addDuration)
					.setInterpolator(
						holder.itemView.context.materialInterpolator(Interpolator.emphasized)
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
				holder.itemView.context.materialInterpolator(Interpolator.emphasizedAccelerated)
			)
	}
	
	override fun removeAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.apply {
			scaleX = 1f
			scaleY = 1f
			alpha = 1f
		}
	}
	
	override fun getAddDelay(remove: Long, move: Long, change: Long): Long = 0L
	
	
	override fun animateMove(
		holder: RecyclerView.ViewHolder,
		x: Boolean,
		y: Boolean,
		additionsPending: Boolean
	): ViewPropertyAnimator {
		return holder.itemView.animate().apply {
			if (x) translationX(0f)
			if (y) translationY(0f)
			
			interpolator = if (additionsPending)
				holder.itemView.context.materialInterpolator(Interpolator.emphasized)
				else holder.itemView.context.materialInterpolator(Interpolator.emphasizedDecelerated)
			duration = if (additionsPending) 200 else moveDuration
		}
	}
	
}