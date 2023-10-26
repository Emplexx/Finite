package moe.emi.aluminium.features.settings.core.deco

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import moe.emi.aluminium.features.settings.core.deco.Deco.EACH_VIEW

/**
 * Calls appropriate drawers for every ViewHolder or RecyclerView
 */
class DecorsBridge(
	private val underlays: List<DecorDrawer<Deco.ViewHolderDecor>>,
//	private val underlaysRecycler: List<RecyclerViewDecor>,
	private val overlays: List<DecorDrawer<Deco.ViewHolderDecor>>,
////	private val overlaysRecycler: List<RecyclerViewDecor>,
	private val offsets: List<DecorDrawer<Deco.OffsetDecor>>
) {
	
	private val groupedUnderlays = underlays.groupBy { it.viewItemType }
	private val groupedOverlays = overlays.groupBy { it.viewItemType }
	private val associatedOffsets = offsets.associateBy { it.viewItemType }
	
	/**
	 * Draws all decors on underlay
	 */
	fun onDrawUnderlay(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
//		underlaysRecycler.drawRecyclerViewDecors(canvas, recyclerView, state)
		groupedUnderlays.drawNotAttachedDecors(canvas, recyclerView, state)
		groupedUnderlays.drawAttachedDecors(canvas, recyclerView, state)
	}
	
	/**
	 * Draws all decors on overlay
	 */
	fun onDrawOverlay(canvas: Canvas, recyclerView: RecyclerView, state: RecyclerView.State) {
		groupedOverlays.drawAttachedDecors(canvas, recyclerView, state)
		groupedOverlays.drawNotAttachedDecors(canvas, recyclerView, state)
//		overlaysRecycler.drawRecyclerViewDecors(canvas, recyclerView, state)
	}
	
	/**
	 * Draws all offset decors
	 */
	fun getItemOffsets(outRect: Rect, view: View, recyclerView: RecyclerView, state: RecyclerView.State) {
		drawOffset(EACH_VIEW, outRect, view, recyclerView, state)
		recyclerView.findContainingViewHolder(view)?.itemViewType?.let { itemViewType ->
			drawOffset(itemViewType, outRect, view, recyclerView, state)
		}
	}
	
	private fun Map<Int, List<DecorDrawer<Deco.ViewHolderDecor>>>.drawAttachedDecors(
		canvas: Canvas,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		
		recyclerView.children.forEach { view ->
			val viewType = recyclerView.getChildViewHolder(view).itemViewType
			this[viewType]?.forEach {
				it.drawer.draw(canvas, view, recyclerView, state)
			}
		}
	}
	
	private fun Map<Int, List<DecorDrawer<Deco.ViewHolderDecor>>>.drawNotAttachedDecors(
		canvas: Canvas,
		recyclerView: RecyclerView,
		state: RecyclerView.State
	) {
		recyclerView.children.forEach { view ->
			this[EACH_VIEW]
				?.forEach { it.drawer.draw(canvas, view, recyclerView, state) }
		}
	}
//
//	private fun List<RecyclerViewDecor>.drawRecyclerViewDecors(
//		canvas: Canvas,
//		recyclerView: RecyclerView,
//		state: RecyclerView.State
//	) {
//		forEach { it.draw(canvas, recyclerView, state) }
//	}
//
	private fun drawOffset(viewType: Int, outRect: Rect, view: View, recyclerView: RecyclerView, state: RecyclerView.State) {
		associatedOffsets[viewType]
			?.drawer
			?.getItemOffsets(outRect, view, recyclerView, state)
	}
}