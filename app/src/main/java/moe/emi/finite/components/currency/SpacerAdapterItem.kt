package moe.emi.finite.components.currency

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemSpacerBinding

class SpacerAdapterItem : BindableItem<ItemSpacerBinding>() {
	
	override fun bind(binding: ItemSpacerBinding, position: Int) {}
	
	override fun getLayout() = R.layout.item_spacer
	override fun initializeViewBinding(view: View) = ItemSpacerBinding.bind(view)
	
}