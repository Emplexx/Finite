package moe.emi.finite.core.ui.decoration

import android.graphics.Canvas
import android.graphics.Outline
import android.view.View
import android.view.ViewOutlineProvider
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView

class RoundDecoration(
	private val radius: Float,
) : RecyclerView.ItemDecoration() {
	
	override fun onDraw(c: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
		super.onDraw(c, recyclerView, state)
		
		if (radius == 0f) return
		
		for (view in recyclerView.children) {
			val vh = recyclerView.getChildViewHolder(view)
			val nextVh =
				recyclerView.findViewHolderForAdapterPosition(vh.bindingAdapterPosition + 1)
			val prevVh =
				recyclerView.findViewHolderForAdapterPosition(vh.bindingAdapterPosition - 1)
			
			val roundMode = getRoundMode(prevVh, vh, nextVh)
			val outlineProvider = view.outlineProvider
			if (outlineProvider is RoundOutlineProvider) {
				outlineProvider.roundMode = roundMode
				view.invalidateOutline()
			} else {
				view.outlineProvider = RoundOutlineProvider(radius, roundMode)
				view.clipToOutline = true
			}
		}
	}
	
	private fun getRoundMode(
		prev: RecyclerView.ViewHolder?,
		curr: RecyclerView.ViewHolder?,
		next: RecyclerView.ViewHolder?
	): RoundMode {
		
		val prevType = prev?.itemViewType ?: -1
		val currType = curr?.itemViewType ?: -1
		val nextType = next?.itemViewType ?: -1
		
		return when {
			prevType != currType && currType != nextType -> RoundMode.All
			prevType != currType && currType == nextType -> RoundMode.Top
			prevType == currType && currType != nextType -> RoundMode.Bottom
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