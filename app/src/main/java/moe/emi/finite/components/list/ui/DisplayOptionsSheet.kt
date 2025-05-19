package moe.emi.finite.components.list.ui

import android.content.Context
import android.graphics.Color
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import convenience.resources.Token
import convenience.resources.colorAttr
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.R
import moe.emi.finite.components.list.domain.Sort
import moe.emi.finite.components.list.getAllPaymentMethods
import moe.emi.finite.components.settings.store.AppSettings
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.settings.store.editor
import moe.emi.finite.core.db.SubscriptionDao
import moe.emi.finite.databinding.LayoutSheetDisplayBinding
import moe.emi.finite.databinding.LayoutSortRowBinding
import moe.emi.finite.databinding.ViewChipFilterBinding
import moe.emi.finite.di.memberInjection
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.textRes

class DisplayOptionsSheet(
	context: Context
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetDisplayBinding
	private var selectedRow: LayoutSortRowBinding? = null
	
	lateinit var subscriptionDao: SubscriptionDao
	lateinit var settingsStore: SettingsStore
	
	init {
		context.memberInjection {
			subscriptionDao = it.db.subscriptionDao()
			settingsStore = it.settingsStore
		}
	}
	
	val editSettings = settingsStore.editor(this)
	
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
			val s = settingsStore.data.first()
			initDisplay(s)
			initSort(s)
			initFilter(s)
		}
	}
	
	private suspend fun initDisplay(s: AppSettings) {
		binding.rowShowTimeLeft.switchView.isChecked = s.showTimeLeft
		binding.rowRoughlySign.switchView.isChecked = s.showRoughlySign
		
		binding.rowShowTimeLeft.switchView.textRes = R.string.option_show_time_left
		binding.rowRoughlySign.switchView.textRes = R.string.option_show_roughly_sign
		
		binding.rowShowTimeLeft.switchView.setOnCheckedChangeListener { _, isChecked ->
			editSettings { it.copy(showTimeLeft = isChecked) }
		}
		binding.rowRoughlySign.switchView.setOnCheckedChangeListener { _, isChecked ->
			editSettings { it.copy(showRoughlySign = isChecked) }
		}
	}
	
	private suspend fun initSort(settings: AppSettings) {
		binding.headerSort.text.textRes = R.string.option_header_sort
		binding.rowName.textLabel.textRes = R.string.option_sort_name
		binding.rowPrice.textLabel.textRes = R.string.option_sort_price
		binding.rowDate.textLabel.textRes = R.string.option_sort_date
		
		selectedRow = when (settings.sort) {
			Sort.Alphabetical -> binding.rowName
			Sort.Price -> binding.rowPrice
			Sort.Date -> binding.rowDate
		}
		selectedRow?.select()
		
		listOf(
			binding.rowName,
			binding.rowPrice,
			binding.rowDate
		).forEach { row ->
			row.root.setOnClickListener { _ ->
				
				if (selectedRow == row) {
					selectedRow?.toggle()
					editSettings { it.copy(sortIsAscending = !it.sortIsAscending) }
				}
				else {
					selectedRow?.unselect()
					selectedRow = row
					selectedRow?.select()
					
					val sort = when (row) {
						binding.rowName -> Sort.Alphabetical
						binding.rowPrice -> Sort.Price
						binding.rowDate -> Sort.Date
						else -> null
					}
					
					editSettings { it.copy(sort = sort ?: it.sort) }
				}
				
				
			}
		}
	}
	
	private suspend fun initFilter(settings: AppSettings) {
		binding.headerFilter.text.text = context.getString(R.string.option_header_filter)

		val payments = getAllPaymentMethods(subscriptionDao).first()
		val selectedPayments = settings.selectedPaymentMethods.map { it.lowercase() }
		
		payments.forEach { string ->
			val chip = ViewChipFilterBinding.inflate(layoutInflater).root
			chip.apply {
				text = string
				isChecked = string.lowercase() in selectedPayments
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
				val asc = settingsStore.data.first().sortIsAscending
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
				
				root.setBackgroundColor(colorAttr(Token.color.primaryContainer))
				textLabel.setTextColor(colorAttr(Token.color.onPrimaryContainer))
//				textHint.children.forEach {
//					(it as? TextView)?.setTextColor(materialColor(TonalColor.onPrimaryContainer))
//				}
			}
			
			val asc = settingsStore.data.first().sortIsAscending
			trailingIcon.rotation = if (asc) 0f else 180f
			trailingIcon.isSelected = asc
			
			trailingIcon.isVisible = true
		}
	}
	
	private fun LayoutSortRowBinding.unselect() {
		with(root.context) {
			
			root.setBackgroundColor(Color.TRANSPARENT)
			textLabel.setTextColor(colorAttr(Token.color.onSurface))

			trailingIcon.isVisible = false
			
		}
	}
}