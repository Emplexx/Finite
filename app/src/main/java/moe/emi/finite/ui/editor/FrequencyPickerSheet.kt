package moe.emi.finite.ui.editor

import android.content.Context
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.chrisbanes.insetter.applyInsetter
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.finite.R
import moe.emi.finite.databinding.LayoutSheetFrequencyBinding
import moe.emi.finite.service.model.BillingPeriod
import moe.emi.finite.service.model.Timespan

class FrequencyPickerSheet(
	context: Context,
	val period: BillingPeriod,
	val onSelect: (BillingPeriod) -> Unit,
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetFrequencyBinding
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetFrequencyBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.root.applyInsetter {
			type(navigationBars = true) {
				margin(bottom = true)
			}
		}
		
		when (period.timespan) {
			Timespan.Week ->
				when (period.count) {
					1 -> binding.rowWeekly
					2 -> binding.row2Weeks
					else -> null
				}
			Timespan.Month ->
				when (period.count) {
					1 -> binding.rowMonthly
					3 -> binding.rowQuarterly
					6 -> binding.row6Months
					else -> null
				}
			Timespan.Year ->
				when (period.count) {
					1 -> binding.rowYearly
					else -> null
				}
			else -> null
		}?.setRowSelected()
		
		binding.rowWeekly.setOnClickListener { selectAndDismiss(1, Timespan.Week) }
		binding.row2Weeks.setOnClickListener { selectAndDismiss(2, Timespan.Week) }
		binding.rowMonthly.setOnClickListener { selectAndDismiss(1, Timespan.Month) }
		binding.rowQuarterly.setOnClickListener { selectAndDismiss(3, Timespan.Month) }
		binding.row6Months.setOnClickListener { selectAndDismiss(6, Timespan.Month) }
		binding.rowYearly.setOnClickListener { selectAndDismiss(1, Timespan.Year) }
	}
	
	private fun selectAndDismiss(every: Int, unit: Timespan) {
		onSelect(BillingPeriod(every, unit))
		this.dismiss()
	}

	
	companion object {
		
		fun TextView.setRowSelected() {
			setBackgroundColor(context.materialColor(TonalColor.primaryContainer))
			setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,R.drawable.ic_check_24,0)
		}
		
	}
}