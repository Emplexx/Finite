package moe.emi.finite.ui.home.adapter

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.viewbinding.GroupieViewHolder

class AutoLayoutDecoration(
	private val key: String,
	private val spaceBetween: Int = 0,
	private val left: Int = 0,
	private val top: Int = 0,
	private val right: Int = 0,
	private val bottom: Int = 0
) : RecyclerView.ItemDecoration() {
	
	override fun getItemOffsets(
		outRect: Rect, view: View,
		recyclerView: RecyclerView, state: RecyclerView.State,
	) {
		
		val holder = recyclerView.getChildViewHolder(view)

		if (holder is GroupieViewHolder<*> && holder.extras.containsKey(key)) {
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
	
}