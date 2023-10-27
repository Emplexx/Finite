package moe.emi.finite.ui.home

import android.view.View
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.ExpandableItem
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemCurrencyBinding

class ExpandableHeaderItem : BindableItem<ItemCurrencyBinding>(), ExpandableItem {
	
	private lateinit var group: ExpandableGroup
	
	override fun bind(binding: ItemCurrencyBinding, position: Int) {
		binding.root.setOnClickListener { group.onToggleExpanded() }
	}
	
	override fun setExpandableGroup(onToggleListener: ExpandableGroup) {
		group = onToggleListener
	}
	override fun getLayout() = R.layout.item_currency
	override fun initializeViewBinding(view: View) = ItemCurrencyBinding.bind(view)
	
	
}