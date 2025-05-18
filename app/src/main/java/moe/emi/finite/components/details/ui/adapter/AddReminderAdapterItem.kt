package moe.emi.finite.components.details.ui.adapter

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemAddReminderBinding

class AddReminderAdapterItem(
	val onClick: () -> Unit,
) : BindableItem<ItemAddReminderBinding>() {
	
	override fun bind(binding: ItemAddReminderBinding, position: Int) {
		binding.root.setOnClickListener { onClick() }
	}
	
	override fun isSameAs(other: Item<*>): Boolean =
		other is AddReminderAdapterItem
	
	override fun hasSameContentAs(other: Item<*>): Boolean =
		other is AddReminderAdapterItem
	
	override fun getLayout() = R.layout.item_add_reminder
	override fun initializeViewBinding(view: View) = ItemAddReminderBinding.bind(view)
	
}