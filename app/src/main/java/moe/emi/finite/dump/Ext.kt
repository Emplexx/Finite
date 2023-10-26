package moe.emi.finite.dump

import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.content.res.Resources
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.pow
import kotlin.math.roundToInt

var View.visible: Boolean
	get() = this.visibility == View.VISIBLE
	set(value) { this.visibility = if (value) View.VISIBLE else View.GONE }

var View.gone: Boolean
	get() = this.visibility == View.GONE
	set(value) { this.visibility = if (value) View.GONE else View.VISIBLE }

var View.invisible: Boolean
	get() = this.visibility == View.INVISIBLE
	set(value) { this.visibility = if (value) View.INVISIBLE else View.VISIBLE }

//fun Double.round(): Double {
//	val df = DecimalFormat("#.##")
//	df.roundingMode = RoundingMode.FLOOR
//	return df.format(this).toDouble()
//}

fun Double.round(decimals: Int): Double {
	val factor = 10.0.pow(decimals.toDouble())
	return (this * factor).roundToInt() / factor
}

fun ViewGroup.enableAnimateChildren(
	dur: Long = 500,
	durVisibility: Long = 200,
	interpolator: TimeInterpolator = FastOutExtraSlowInInterpolator(),
	interpolatorVisibility: TimeInterpolator = FastOutSlowInInterpolator(),
	animateParent: Boolean = false
) {
	
	layoutTransition = LayoutTransition()
	layoutTransition.apply {
		
		enableTransitionType(LayoutTransition.CHANGING)
		enableTransitionType(LayoutTransition.APPEARING)
		enableTransitionType(LayoutTransition.DISAPPEARING)
		enableTransitionType(LayoutTransition.CHANGE_APPEARING)
		enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
		
		setDuration(dur)
		setDuration(LayoutTransition.APPEARING, durVisibility)
		setDuration(LayoutTransition.DISAPPEARING, durVisibility)
		
		setInterpolator(LayoutTransition.CHANGING, interpolator)
		setInterpolator(LayoutTransition.APPEARING, interpolatorVisibility)
		setInterpolator(LayoutTransition.DISAPPEARING, interpolatorVisibility)
		setInterpolator(LayoutTransition.CHANGE_APPEARING, interpolator)
		setInterpolator(LayoutTransition.CHANGE_DISAPPEARING, interpolator)
		
		setAnimateParentHierarchy(animateParent)
	}
	
}

@ColorInt
fun Int.copy(
	alpha: Int? = null,
	r: Int? = null,
	g: Int? = null,
	b: Int? = null,
): Int {
	return Color.argb(
		alpha ?: Color.alpha(this),
		r ?: Color.red(this),
		g ?: Color.green(this),
		b ?: Color.blue(this)
	)
}

@ColorInt
fun Int.alpha(
	alpha: Float
) = this.copy((alpha * 255).roundToInt())

val Number.fSp get() = TypedValue.applyDimension(
	TypedValue.COMPLEX_UNIT_SP,
	this.toFloat(),
	Resources.getSystem().displayMetrics
)

val Number.fDp get() = TypedValue.applyDimension(
	TypedValue.COMPLEX_UNIT_DIP,
	this.toFloat(),
	Resources.getSystem().displayMetrics
)

val Number.iDp get() = this.fDp.toInt()


@OptIn(ExperimentalContracts::class)
inline fun <T1: Any, T2: Any, R: Any> safe(p1: T1?, p2: T2?, block: (T1, T2) -> R?): R? {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return if (p1 != null && p2 != null) block(p1, p2) else null
}