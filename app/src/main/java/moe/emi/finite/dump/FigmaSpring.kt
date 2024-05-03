package moe.emi.finite.dump

import androidx.dynamicanimation.animation.SpringAnimation
import kotlin.math.sqrt

object FigmaSpring {
	
	fun SpringAnimation.fromFigmaConfig(
		stiffness: Float,
		damping: Float,
		mass: Float
	) = this.apply {
		spring.stiffness = stiffness
		spring.dampingRatio = damping / (2 * sqrt(stiffness * mass)) // TODO Limit this to 1f as figma does not allow overdamped springs
	}
	
	/**
	 * Configure this spring animation to be the same as the "Quick" preset in Figma
	 */
	fun SpringAnimation.configFigmaQuick() = this.apply {
		this.fromFigmaConfig(300f, 20f, 1f)
	}
	
	/**
	 * Configure this spring animation to be the same as the "Gentle" preset in Figma
	 */
	fun SpringAnimation.configFigmaGentle() = this.apply {
		this.fromFigmaConfig(100f, 15f, 1f)
	}
}