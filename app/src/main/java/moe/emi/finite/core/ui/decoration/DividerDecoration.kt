package moe.emi.finite.core.ui.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import moe.emi.finite.R
import kotlin.math.roundToInt

fun RecyclerView.divider(builder: DividerConfig.() -> Unit) {
	addItemDecoration(dividerDecoration(context, builder))
}

fun dividerDecoration(context: Context, builder: DividerConfig.() -> Unit): DividerDecoration {
	val config = DividerConfig().apply(builder)
	return config.makeDecoration(context)
}

fun RecyclerView.showBetween(viewType: Int): (Int) -> Boolean {
	return { childIndex: Int ->
		val vh = getChildAt(childIndex)?.let(::getChildViewHolder)
		val next = getChildAt(childIndex + 1)?.let(::getChildViewHolder)
		vh?.itemViewType == viewType && next?.itemViewType == viewType
	}
}

data class DividerConfig(
	private var _paddingStart: (childIndex: Int) -> Int = { 0 },
	var paddingEnd: Int = 0,
	
	var background: Boolean = false,
	var makeSpace: Boolean = true,
) {
	
	var paddingStart: Int
		@Deprecated("", level = DeprecationLevel.ERROR)
		get() = error("")
		set(value) {
			_paddingStart = { value }
		}
	
	fun paddingStart(predicate: (childIndex: Int) -> Int) {
		_paddingStart = predicate
	}
	
	var showUnder: (Int) -> Boolean  = { true }
		private set
	
	fun showUnder(predicate: (childIndex: Int) -> Boolean) {
		showUnder = predicate
	}
	
	fun makeDecoration(context: Context) = DividerDecoration(
		context,
		paddingStart = _paddingStart,
		paddingEnd = paddingEnd,
		showUnder = showUnder,
		background = background,
		makeSpace = makeSpace
	)
	
}

class DividerDecoration(
	context: Context,
	val paddingStart: (childIndex: Int) -> Int = { 0 },
	val paddingEnd: Int = 0,
	var background: Boolean = false,
	val makeSpace: Boolean = true,
	val showUnder: (childIndex: Int) -> Boolean = { true },
) : RecyclerView.ItemDecoration() {
	
	val drawable = ResourcesCompat.getDrawable(context.resources, R.drawable.divider, context.theme)!!
	val drawableBg = ResourcesCompat.getDrawable(context.resources, R.drawable.divider, context.theme)!!
	private val mBounds = Rect()
	
	override fun onDraw(
		canvas: Canvas,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		for (i in 0 until parent.childCount - 1) {
			
			val paddingStart = paddingStart(i)
			
			val left: Int
			val right: Int
			if (parent.clipToPadding) {
				left = parent.paddingLeft + paddingStart
				right = parent.width - parent.paddingRight - paddingEnd
				canvas.clipRect(
					left, parent.paddingTop, right,
					parent.height - parent.paddingBottom
				)
			} else {
				left = 0 + paddingStart
				right = parent.width - paddingEnd
			}
			
			if (showUnder(i)) {
				val child = parent.getChildAt(i)
				
				drawable.alpha = (child.alpha * 255).roundToInt()
				drawableBg.alpha = (child.alpha * 255).roundToInt()
				
				parent.getDecoratedBoundsWithMargins(child, mBounds)
				val bottom: Int = mBounds.bottom + child.translationY.roundToInt()
				
				val top: Int = bottom - drawable.intrinsicHeight
				
				if (background) {
					drawableBg.setBounds(parent.paddingLeft, top, parent.width, bottom)
					drawableBg.draw(canvas)
				}
				
				drawable.setBounds(left, top, right, bottom)
				drawable.draw(canvas)
			}
		}
	}
	
	override fun onDrawOver(
		canvas: Canvas,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
	
	}
	
	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		parent: RecyclerView,
		state: RecyclerView.State
	) {
		
//		if (showUnder(parent.indexOfChild(view)))
			if (makeSpace) outRect.set(0, 0, 0, drawable.intrinsicHeight)
		// TODO don't apply to bottom view
	}
	
}