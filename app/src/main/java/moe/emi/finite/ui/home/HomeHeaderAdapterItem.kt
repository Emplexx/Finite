package moe.emi.finite.ui.home

import android.view.View
import com.robinhood.ticker.TickerUtils
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemHomeHeaderBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.enableAnimateChildren
import moe.emi.finite.dump.round
import moe.emi.finite.service.data.Currency
import kotlin.math.round

class HomeHeaderAdapterItem(
	private var view: TotalView,
	private var amount: Double,
	private var preferredCurrency: Currency,
	private val onClick: () -> Unit,
) : BindableItem<ItemHomeHeaderBinding>() {
	
	fun updateTotal(view: TotalView, amount: Double, currency: Currency) {
		val changed = this.amount != amount
		this.view = view
		this.amount = amount
		this.preferredCurrency = currency
		if (changed) this.notifyChanged("Total")
	}
	
	override fun bind(binding: ItemHomeHeaderBinding, position: Int) {
		
//		binding.root.enableAnimateChildren()
		binding.textTotal.setCharacterLists("0123456789")
		binding.textTotal.animationInterpolator = FastOutExtraSlowInInterpolator()
		
		
		bindTotal(binding)
		
		binding.root.setOnClickListener { onClick() }
	}
	
	private fun bindTotal(binding: ItemHomeHeaderBinding) {
		binding.textCurrencySign.text = preferredCurrency.symbol ?: preferredCurrency.iso4217Alpha
		binding.textView.text = when (view) {
			TotalView.Yearly -> R.string.period_yearly
			TotalView.Monthly -> R.string.period_monthly
			TotalView.Weekly -> R.string.period_weekly
		}.let { binding.root.context.getString(it) }
		binding.textTotal.setText(amount.round(2).toString(), true)
	}
	
	override fun bind(
		viewBinding: ItemHomeHeaderBinding,
		position: Int,
		payloads: MutableList<Any>
	) {
		for (i in payloads) {
			when (i) {
				"Total" -> bindTotal(viewBinding)
			}
		}
		if (payloads.isEmpty()) super.bind(viewBinding, position, payloads)
	}
	
	override fun isRecyclable(): Boolean = false
	
	override fun getLayout() = R.layout.item_home_header
	override fun initializeViewBinding(view: View) = ItemHomeHeaderBinding.bind(view)
	
}