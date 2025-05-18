package moe.emi.finite.components.currency

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import convenience.resources.drawable
import moe.emi.finite.R
import moe.emi.finite.core.model.Currency
import moe.emi.finite.databinding.ItemCurrencyBinding

class CurrencyAdapterItem(
	val currency: Currency,
	val onClick: () -> Unit,
) : BindableItem<ItemCurrencyBinding>() {
	
	override fun bind(binding: ItemCurrencyBinding, position: Int) {
		with(binding.root.context) {
			binding.textName.text = currency.fullName
			binding.textCode.text = currency.iso4217Alpha
			
			binding.icon.setImageDrawable(
				currency.flag?.let { this.drawable(it) }
			)
			
			binding.root.setOnClickListener { onClick() }
		}
	}
	
	override fun isSameAs(other: Item<*>): Boolean =
		when (other) {
			is CurrencyAdapterItem -> currency.iso4217Alpha == other.currency.iso4217Alpha
			else -> super.isSameAs(other)
		}
	
	override fun hasSameContentAs(other: Item<*>): Boolean =
		when (other) {
			is CurrencyAdapterItem -> currency == other.currency
			else -> super.isSameAs(other)
		}
	
	override fun getLayout() = R.layout.item_currency
	override fun initializeViewBinding(view: View) = ItemCurrencyBinding.bind(view)
	
}