package moe.emi.aluminium.features.settings.core.deco

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.recyclerview.widget.RecyclerView
import moe.emi.aluminium.features.settings.core.deco.Rules.END
import moe.emi.aluminium.features.settings.core.deco.Rules.MIDDLE
import moe.emi.convenience.drawable
import moe.emi.finite.R

class DecorDrawer<D>(val viewItemType: Int, val drawer: D)

class MasterDecorator(private val decorsBridge: DecorsBridge) : RecyclerView.ItemDecoration() {
	
	override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		super.onDrawOver(canvas, parent, state)
		decorsBridge.onDrawOverlay(canvas, parent, state)
	}
	
	override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
		super.onDraw(canvas, parent, state)
		decorsBridge.onDrawUnderlay(canvas, parent, state)
	}
	
	override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
		super.getItemOffsets(outRect, view, parent, state)
		decorsBridge.getItemOffsets(outRect, view, parent, state)
	}
}

object Deco {
	
	const val EACH_VIEW = -1
	
	class Builder {
		
		private var underlayViewHolderScope: MutableList<DecorDrawer<ViewHolderDecor>> =
			mutableListOf()
		
		private var overlayViewHolderScope: MutableList<DecorDrawer<ViewHolderDecor>> =
			mutableListOf()
		
		private var offsetsScope: MutableList<DecorDrawer<OffsetDecor>> =
			mutableListOf()
		
		fun underlay(decor: ViewHolderDecor): Builder {
			return apply {
				underlayViewHolderScope.add(DecorDrawer(EACH_VIEW, decor))
			}
		}
		
		fun overlay(decor: ViewHolderDecor): Builder {
			return apply {
				overlayViewHolderScope.add(DecorDrawer(EACH_VIEW, decor))
			}
		}
		
		fun offset(decor: OffsetDecor): Builder {
			return apply {
				offsetsScope.add(DecorDrawer(EACH_VIEW, decor))
			}
		}
		
		fun build(): MasterDecorator {
//			require(
//				offsetsScope.groupingBy { it.viewItemType }.eachCount().all { it.value == 1 }
//			) { "Any ViewHolder can have only a single OffsetDrawer" }
			
			return MasterDecorator(
				DecorsBridge(
					underlayViewHolderScope,
//					underlayRecyclerScope,
					overlayViewHolderScope,
//					overlayRecyclerScope,
					offsetsScope
				)
			)
		}
	}
	
	interface OffsetDecor {
		fun getItemOffsets(outRect: Rect, view: View, recyclerView: RecyclerView, state: RecyclerView.State)
	}
	interface ViewHolderDecor {
		fun draw(canvas: Canvas, view: View, recyclerView: RecyclerView, state: RecyclerView.State)
	}
}

class RoundDecor(
	private val cornerRadius: Float,
//	private val roundPolitic: RoundPolitic = Every(RoundMode.ALL)
) : Deco.ViewHolderDecor {
	
	override fun draw(
		canvas: Canvas,
		view: View,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		
		val viewHolder = recyclerView.getChildViewHolder(view)
		val nextViewHolder =
			recyclerView.findViewHolderForAdapterPosition(viewHolder.bindingAdapterPosition + 1)
		val previousChildViewHolder =
			recyclerView.findViewHolderForAdapterPosition(viewHolder.bindingAdapterPosition - 1)
		
		if (cornerRadius.compareTo(0f) != 0) {
			val roundMode = getRoundMode(previousChildViewHolder, viewHolder, nextViewHolder)
			val outlineProvider = view.outlineProvider
			if (outlineProvider is RoundOutlineProvider) {
				outlineProvider.roundMode = roundMode
				view.invalidateOutline()
			} else {
				view.outlineProvider = RoundOutlineProvider(cornerRadius, roundMode)
				view.clipToOutline = true
			}
		}
	}
	
	private fun getRoundMode(
		previousChildViewHolder: RecyclerView.ViewHolder?,
		currentViewHolder: RecyclerView.ViewHolder?,
		nextChildViewHolder: RecyclerView.ViewHolder?
	): RoundMode {
		
		val previousHolderItemType = previousChildViewHolder?.itemViewType ?: -1
		val currentHolderItemType = currentViewHolder?.itemViewType ?: -1
		val nextHolderItemType = nextChildViewHolder?.itemViewType ?: -1
		
		return when {
			previousHolderItemType != currentHolderItemType && currentHolderItemType != nextHolderItemType -> RoundMode.All
			previousHolderItemType != currentHolderItemType && currentHolderItemType == nextHolderItemType -> RoundMode.Top
			previousHolderItemType == currentHolderItemType && currentHolderItemType != nextHolderItemType -> RoundMode.Bottom
			previousHolderItemType == currentHolderItemType && currentHolderItemType == nextHolderItemType -> RoundMode.None
			else -> RoundMode.None
		}
	}
}

enum class RoundMode {
	All, Top, Bottom, None
}

class RoundOutlineProvider(
	var outlineRadius: Float = 0f,
	var roundMode: RoundMode = RoundMode.None
) : ViewOutlineProvider() {
	
	private val topOffset
		get() = when (roundMode) {
			RoundMode.All, RoundMode.Top -> 0
			RoundMode.None, RoundMode.Bottom -> cornerRadius.toInt()
		}
	private val bottomOffset
		get() = when (roundMode) {
			RoundMode.All, RoundMode.Bottom -> 0
			RoundMode.None, RoundMode.Top -> cornerRadius.toInt()
		}
	private val cornerRadius
		get() = if (roundMode == RoundMode.None) {
			0f
		} else {
			outlineRadius
		}
	
	override fun getOutline(view: View, outline: Outline) {
		outline.setRoundRect(
			0,
			0 - topOffset,
			view.width,
			view.height + bottomOffset,
			cornerRadius
		)
	}
}



class AutoLayoutDecor(
	private val spaceBetween: Int = 0,
	private val left: Int = 0,
	private val top: Int = 0,
	private val right: Int = 0,
	private val bottom: Int = 0
) : Deco.OffsetDecor {
	
	constructor(padding: Int) : this(0, padding, padding, padding, padding)
	constructor(spaceBetween: Int, padding: Int) : this(spaceBetween, padding, padding, padding, padding)
	
	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		if (recyclerView.getChildAdapterPosition(view) == 0) {
			outRect.top = top
		}
		
		if (recyclerView.getChildAdapterPosition(view) == state.itemCount-1) {
			outRect.bottom = bottom
		} else
			outRect.bottom = spaceBetween
		
		outRect.left = left
		outRect.right = right
	}
}


@Retention(AnnotationRetention.SOURCE)
@IntDef(
	Rules.MIDDLE,
	Rules.END
)
annotation class DividerRule

object Rules {
	const val MIDDLE = 1
	const val END = 2
	
	fun checkMiddleRule(rule: Int): Boolean {
		return rule and MIDDLE != 0
	}
	
	fun checkEndRule(rule: Int): Boolean {
		return rule and END != 0
	}
	
	fun checkAllRule(rule: Int): Boolean {
		return rule and (MIDDLE or END) != 0
	}
}

@SuppressLint("WrongConstant")
class Gap(
	@ColorInt val color: Int = Color.TRANSPARENT,
	val height: Float = 0f,
	val paddingStart: Int = 0,
	val paddingEnd: Int = 0,
	@DividerRule val rule: Int = MIDDLE or END
)

class LinearDividerDrawer(private val gap: Gap) : Deco.ViewHolderDecor {
	
	private val dividerPaint = Paint()
	private val alpha = dividerPaint.alpha
	
	init {
		dividerPaint.color = gap.color
		dividerPaint.strokeWidth = gap.height
	}
	
	override fun draw(
		canvas: Canvas,
		view: View,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		val viewHolder = recyclerView.getChildViewHolder(view)
		val nextViewHolder = recyclerView.findViewHolderForAdapterPosition(viewHolder.bindingAdapterPosition + 1)
		
		val startX = recyclerView.paddingLeft + gap.paddingStart
		val startY = view.bottom + view.translationY
		val stopX = recyclerView.width - recyclerView.paddingRight - gap.paddingEnd
		val stopY = startY
		
		dividerPaint.alpha = (view.alpha * alpha).toInt()
		
		val areSameHolders =
			viewHolder.itemViewType == (nextViewHolder?.itemViewType ?: -1)
		
		val drawMiddleDivider = Rules.checkMiddleRule(gap.rule) && areSameHolders
		val drawEndDivider = Rules.checkEndRule(gap.rule) && areSameHolders.not()
		
		if (drawMiddleDivider) {
			canvas.drawLine(startX.toFloat(), startY, stopX.toFloat(), stopY, dividerPaint)
		} else if (drawEndDivider) {
			canvas.drawLine(startX.toFloat(), startY, stopX.toFloat(), stopY, dividerPaint)
		}
	}
}


class DividerDeco(
	context: Context,
	val paddingStart: Int,
	val paddingEnd: Int,
) : Deco.ViewHolderDecor, Deco.OffsetDecor {
	
	val drawable = //GradientDrawable().apply {
//		shape = GradientDrawable.RECTANGLE
//		color = ColorStateList.valueOf(context.materialColor(TonalColor.outline))
//		setSize(-1, 3)
//	}
		context.drawable(R.drawable.divider)
	
	private val mBounds = Rect()
	
	override fun draw(
		canvas: Canvas,
		view: View,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		recyclerView.layoutManager ?: return
		
		canvas.save()
		
		val left: Int
		val right: Int
		//noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
		if (recyclerView.clipToPadding) {
			left = recyclerView.paddingLeft + paddingStart
			right = recyclerView.width - recyclerView.paddingRight - paddingEnd
			canvas.clipRect(
				left, recyclerView.paddingTop, right,
				recyclerView.height - recyclerView.paddingBottom
			)
		} else {
			left = 0 + paddingStart
			right = recyclerView.width - paddingEnd
		}
		
		val parent = recyclerView
		
		val childCount: Int = parent.childCount
		for (i in 0 until childCount - 1) {
			val child: View = parent.getChildAt(i)
			parent.getDecoratedBoundsWithMargins(child, mBounds)
			val bottom: Int = mBounds.bottom + Math.round(child.translationY)
			val top: Int = bottom - drawable.getIntrinsicHeight()
			drawable.setBounds(left, top, right, bottom)
			drawable.draw(canvas)
		}
	}
	
	override fun getItemOffsets(
		outRect: Rect,
		view: View,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		outRect.set(0, 0, 0, drawable.intrinsicHeight);
	}
}
