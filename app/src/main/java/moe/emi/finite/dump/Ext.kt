package moe.emi.finite.dump

import android.animation.ArgbEvaluator
import android.animation.LayoutTransition
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.animation.doOnEnd
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.pow
import kotlin.math.roundToInt

var View.visible: Boolean
	get() = this.visibility == View.VISIBLE
	set(value) { this.visibility = if (value) View.VISIBLE else View.GONE }

//var View.gone: Boolean
//	get() = this.visibility == View.GONE
//	set(value) { this.visibility = if (value) View.GONE else View.VISIBLE }

//var View.invisible: Boolean
//	get() = this.visibility == View.INVISIBLE
//	set(value) { this.visibility = if (value) View.INVISIBLE else View.VISIBLE }

@get:CheckResult("Set-only property. Getting this property is an error and will throw")
var <T : TextView> T.textRes: Int
	get() { error("set-only property") }
	set(value) = this.setText(value)

//context(Context)
//fun @receiver:StringRes Int.get() = this@Context.getString(this@Int)

//infix fun @receiver:StringRes Int.from(context: Context) = context.getString(this@Int)

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

fun ImageView.animatedDrawable(@DrawableRes drawableId: Int, context: Context) {
	val animDrawable: AnimatedVectorDrawableCompat? =
		AnimatedVectorDrawableCompat.create(context, drawableId)
	
	this.setImageDrawable(animDrawable)
	val animatable: Drawable? = this.drawable
	
	if (animatable is Animatable) {
		animatable.start()
	}
}

@CheckResult
fun colorAnimator(
	@ColorInt from: Pair<Int, Int>,
	duration: Long,
	onEnd: () -> Unit = {},
	onUpdate: (ValueAnimator) -> Unit,
) = ValueAnimator.ofObject(ArgbEvaluator(), from.first, from.second).apply {
	this.duration = duration
	this.addUpdateListener(onUpdate)
	this.doOnEnd { onEnd() }
}