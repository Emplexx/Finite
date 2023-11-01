package moe.emi.finite.ui.details

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemUpcomingPaymentBinding
import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.Timespan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class UpcomingPaymentAdapterItem(
	val date: LocalDate,
	val period: BillingPeriod
) : BindableItem<ItemUpcomingPaymentBinding>() {
	
	override fun bind(binding: ItemUpcomingPaymentBinding, position: Int) {
		if (period >= BillingPeriod(1, Timespan.Year)) {
			binding.textSmall.text = DateTimeFormatter.ofPattern("YYYY").format(date)
			binding.textLarge.text = DateTimeFormatter.ofPattern("MMM d").format(date)
			
		} else {
			binding.textSmall.text = DateTimeFormatter.ofPattern("MMM").format(date)
			binding.textLarge.text = DateTimeFormatter.ofPattern("d").format(date)
		}
		
	}
	
	override fun getLayout() = R.layout.item_upcoming_payment
	override fun initializeViewBinding(view: View) = ItemUpcomingPaymentBinding.bind(view)
	
}