package moe.emi.finite.ui.home

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.finite.R
import moe.emi.finite.databinding.LayoutSheetDisplayBinding
import moe.emi.finite.databinding.LayoutSortRowBinding
import moe.emi.finite.databinding.ViewChipFilterBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.textRes
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.editSettings
import moe.emi.finite.service.datastore.set
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.home.model.Sort

class DisplayOptionsSheet(
	context: Context
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetDisplayBinding
	private var selectedRow: LayoutSortRowBinding? = null
	private var isAscending = true
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetDisplayBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
		behavior.skipCollapsed = true
		
		lifecycleScope.launch {
			initDisplay()
			initSort()
			initFilter()
		}
	}
	
	private suspend fun initDisplay() {
		val s = context.appSettings.first()
		binding.rowShowTimeLeft.switchView.isChecked = s.showTimeLeft
		binding.rowRoughlySign.switchView.isChecked = s.showRoughlySign
		
		binding.rowShowTimeLeft.switchView.textRes = R.string.option_show_time_left
		binding.rowRoughlySign.switchView.textRes = R.string.option_show_roughly_sign
		
		binding.rowShowTimeLeft.switchView.setOnCheckedChangeListener { _, isChecked ->
			lifecycleScope.launch {
				context.appSettings.first().copy(showTimeLeft = isChecked).set()
			}
		}
		binding.rowRoughlySign.switchView.setOnCheckedChangeListener { _, isChecked ->
			lifecycleScope.launch {
				context.appSettings.first().copy(showRoughlySign = isChecked).set()
			}
		}
	}
	
	private suspend fun initSort() {
		binding.headerSort.text.textRes = R.string.option_header_sort
		binding.rowName.textLabel.textRes = R.string.option_sort_name
		binding.rowPrice.textLabel.textRes = R.string.option_sort_price
		binding.rowDate.textLabel.textRes = R.string.option_sort_date
		
		selectedRow = when (context.appSettings.first().sort) {
			Sort.Alphabetical -> binding.rowName
			Sort.Price -> binding.rowPrice
			Sort.Date -> binding.rowDate
		}
		selectedRow?.select()
		isAscending = context.appSettings.first().sortIsAscending
		
		listOf(
			binding.rowName,
			binding.rowPrice,
			binding.rowDate,
		).forEach {
			it.root.setOnClickListener { _ ->
				
				if (selectedRow == it) {
					selectedRow?.toggle()
					
					lifecycleScope.launch {
						val ascending = isAscending
						context.appSettings.first().copy(sortIsAscending = !ascending).set()
						isAscending = !ascending
					}
				}
				else {
					selectedRow?.unselect()
					selectedRow = it
					selectedRow?.select()
					
					when (it) {
						binding.rowName -> Sort.Alphabetical
						binding.rowPrice -> Sort.Price
						binding.rowDate -> Sort.Date
						else -> null
					}?.let { lifecycleScope.launch {
						context.appSettings.first().copy(sort = it).set()
					} }
				}
				
				
			}
		}
	}
	
	private suspend fun initFilter() {
		binding.headerFilter.text.text = context.getString(R.string.option_header_filter)

		val payments = SubscriptionsRepo.getAllSubscriptions().first()
			.map { it.paymentMethod.trim() }
			.filter { it.isNotBlank() }
			.associateBy {
				it.lowercase()
			}
			.values
			.toSet()
		val selectedPayments = context.appSettings.first().selectedPaymentMethods
		
		payments.forEach { string ->
			val chip = ViewChipFilterBinding.inflate(layoutInflater).root
			chip.apply {
				text = string
				isChecked = string.lowercase() in selectedPayments.map { it.lowercase() }
				setOnCheckedChangeListener { _, isChecked ->
					editSettings {
						it.copy(
							selectedPaymentMethods = if (isChecked) it.selectedPaymentMethods + string.lowercase()
							else it.selectedPaymentMethods - string.lowercase()
						)
					}
				}
			}
			binding.chipGroupPayment.addView(chip)
		}
	}
	
	private var jobReturnArrow: Job? = null
	private fun LayoutSortRowBinding.toggle() {
		jobReturnArrow?.cancel()
		lifecycleScope.launch {
			
			trailingIcon.animate()
				.setInterpolator(FastOutExtraSlowInInterpolator())
				.setDuration(500)
				.rotationBy(180f)
			
			jobReturnArrow = launch {
				delay(1000)
				val asc = context.appSettings.first().sortIsAscending
				val rem = trailingIcon.rotation.toInt() % 180
				if (rem != 0) {
					
					if (trailingIcon.rotation > 360f * 2) {
						trailingIcon.rotation = trailingIcon.rotation % 360f
					}
					
					trailingIcon.animate()
						.setInterpolator(FastOutExtraSlowInInterpolator())
						.setDuration(500)
						.rotation(if (asc) 0f else 180f)
				}
			}
		}
	}
	
	private fun LayoutSortRowBinding.select() {
		lifecycleScope.launch {
			with(root.context) {
				
				root.setBackgroundColor(materialColor(TonalColor.primaryContainer))
				textLabel.setTextColor(materialColor(TonalColor.onPrimaryContainer))
//				textHint.children.forEach {
//					(it as? TextView)?.setTextColor(materialColor(TonalColor.onPrimaryContainer))
//				}
			}
			
			val asc = context.appSettings.first().sortIsAscending
			trailingIcon.rotation = if (asc) 0f else 180f
			trailingIcon.isSelected = asc
			
			trailingIcon.visible = true
		}
	}
	
	private fun LayoutSortRowBinding.unselect() {
		with(root.context) {
			
			root.setBackgroundColor(Color.TRANSPARENT)
			textLabel.setTextColor(materialColor(TonalColor.onSurface))

			trailingIcon.visible = false
			
		}
	}
}