package moe.emi.finite.ui.home.adapter.java

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import com.xwray.groupie.Group
import com.xwray.groupie.Section
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.convenience.drawable
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemCollapsibleHeaderBinding
import moe.emi.finite.dump.animatedDrawable

class ExpandableHeaderItem : BindableItem<ItemCollapsibleHeaderBinding>(), RealItem {
	
	private lateinit var group: ExpandableSection
	
	override fun bind(binding: ItemCollapsibleHeaderBinding, position: Int) {
		binding.text.text = "Inactive"
		
		binding.cuteArrow.setImageDrawable(binding.root.context.drawable(
			if (!group.isExpanded) {
				R.drawable.ic_caret_down_static
			} else R.drawable.ic_caret_up_static
		))
		
		binding.root.setOnClickListener {
			group.toggle()
			
			if (!group.isExpanded) {
				binding.cuteArrow.animatedDrawable(R.drawable.ic_caret_down_animation, binding.root.context)
			} else {
				binding.cuteArrow.animatedDrawable(R.drawable.ic_caret_up_animation, binding.root.context)
			}
		}
	}
	
	override fun setGroup(group: ExpandableSection) {
		this.group = group
	}
	override fun getLayout() = R.layout.item_collapsible_header
	override fun initializeViewBinding(view: View) = ItemCollapsibleHeaderBinding.bind(view)
	
	
}

interface RealItem {
	fun setGroup(group: ExpandableSection)
}

class ExpandableSection(
	headerItem: ExpandableHeaderItem,
	var isExpanded: Boolean = true
) : Section() {
	
	val elWiwi = Section()
	
	init {
		headerItem.setGroup(this)
		this.setHeader(headerItem)
		if (isExpanded) this.add(elWiwi)
	}
	
	fun toggle() {
		if (isExpanded) {
			this.clear()
		} else {
			this.add(elWiwi)
		}
		isExpanded = !isExpanded
	}
	
	override fun getGroups(): MutableList<Group> {
		return elWiwi.groups
	}
	
	override fun update(
		newBodyGroups: Collection<out Group>,
		diffResult: DiffUtil.DiffResult?
	) {
		elWiwi.update(newBodyGroups, diffResult)
	}
	
	override fun update(newBodyGroups: Collection<out Group>) {
		elWiwi.update(newBodyGroups)
	}
	
	override fun update(newBodyGroups: Collection<out Group>, detectMoves: Boolean) {
		elWiwi.update(newBodyGroups, detectMoves)
	}
	
	public override fun isEmpty(): Boolean {
		return elWiwi.groups.isEmpty() || GroupUtils.getItemCount(elWiwi.groups) == 0
	}
	
}