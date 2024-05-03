package moe.emi.finite.ui.home.adapter.anim

import android.view.View
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.FloatPropertyCompat
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.recyclerview.widget.RecyclerView
import moe.emi.finite.dump.FigmaSpring.configFigmaGentle
import moe.emi.finite.dump.FigmaSpring.configFigmaQuick

internal open class SpringItemAnimator : BaseItemAnimator<SpringAnimation>() {
	
	private object Scale : FloatPropertyCompat<View>("scale") {
		override fun getValue(view: View): Float {
			return view.scaleX
		}
		
		override fun setValue(view: View, value: Float) {
			view.scaleX = value
			view.scaleY = value
		}
	}
	
	
	// Remove
	
	override fun removeAnimation(holder: RecyclerView.ViewHolder): SpringAnimation {
		return SpringAnimation(holder?.itemView, Scale, 0f).configFigmaQuick()
	}
	
	override fun removeAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder.itemView.apply {
			scaleX = 1f
			scaleY = 1f
		}
	}
	
	
	// Add
	
	override fun addAnimationPrepare(holder: RecyclerView.ViewHolder) {
		holder?.itemView?.apply {
			scaleX = 0f
			scaleY = 0f
		}
	}
	
	override fun addAnimation(holder: RecyclerView.ViewHolder, movesPending: Boolean): SpringAnimation {
		return SpringAnimation(holder?.itemView, Scale, 1f).configFigmaQuick()
	}
	
	override fun addAnimationCleanup(holder: RecyclerView.ViewHolder) {
		holder?.itemView?.apply {
			scaleX = 1f
			scaleY = 1f
		}
	}
	
	
	// Change
	
	override fun changeOldAnimation(
		holder: RecyclerView.ViewHolder,
		changeInfo: ChangeInfo?
	): SpringAnimation {
		return SpringAnimation(
			holder?.itemView,
			SpringAnimation.TRANSLATION_Y,
			(changeInfo!!.toY - changeInfo.fromY).toFloat()
		)
	}
	
	override fun changeNewAnimation(holder: RecyclerView.ViewHolder): SpringAnimation {
		return SpringAnimation(
			holder?.itemView,
			SpringAnimation.TRANSLATION_Y,
			0f
		)
	}
	
	override fun changeAnimationCleanup(holder: RecyclerView.ViewHolder?) {
	
	}
	
	
	// Move
	
	override fun animateMove(
		holder: RecyclerView.ViewHolder,
		x: Boolean,
		y: Boolean,
		additionsPending: Boolean
	): SpringAnimation {
		return if (y) SpringAnimation(holder.itemView, DynamicAnimation.TRANSLATION_Y, 0f).configFigmaGentle()
		else SpringAnimation(holder.itemView, DynamicAnimation.TRANSLATION_X, 0f)
	}
	
	
	// Spring
	
	val startListeners = mutableMapOf<Int, () -> Unit>()
	
	override fun setupAnimationListener(
		animation: SpringAnimation,
		onStart: () -> Unit,
		onEnd: () -> Unit,
		onCancel: () -> Unit
	) {
		startListeners[animation.hashCode()] = onStart
		animation.addEndListener { _, canceled, value, velocity ->
			if (canceled) onCancel() else onEnd()
		}
	}
	
	override fun cleanupAnimationListener(animation: SpringAnimation) {
		// TODO ??
	}
	
	override fun startAnimation(animation: SpringAnimation) {
		startListeners[animation.hashCode()]!!()
		animation.start()
	}
}