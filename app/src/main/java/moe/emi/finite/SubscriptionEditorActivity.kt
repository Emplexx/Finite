package moe.emi.finite

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import codes.side.andcolorpicker.converter.toColorInt
import codes.side.andcolorpicker.model.IntegerHSLColor
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import moe.emi.convenience.materialColor
import moe.emi.finite.databinding.ActivitySubscriptionEditorBinding
import moe.emi.finite.dump.alpha
import moe.emi.finite.dump.visible
import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.FullDate
import moe.emi.finite.ui.currency.CurrencyPickerSheet
import moe.emi.finite.ui.editor.FrequencyPickerSheet
import moe.emi.finite.ui.editor.showColorPickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class SubscriptionEditorActivity : AppCompatActivity() {
	
	private val viewModel by viewModels<SubscriptionEditorViewModel>()
	private lateinit var binding: ActivitySubscriptionEditorBinding
	
	private val callback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			if (viewModel.subscription.id == 0) {
				// TODO save draft
				finishWithMessage()
			} else {
				// TODO compare changes and alert dialog
				this.isEnabled = false
				onBackPressedDispatcher.onBackPressed()
			}
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivitySubscriptionEditorBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		onBackPressedDispatcher.addCallback(callback)
		
		initViews()
	}
	
	private fun initViews() {
		binding.toolbar.setNavigationOnClickListener {
			onBackPressedDispatcher.onBackPressed()
		}
		
		binding.fab.visible = viewModel.canSave
		binding.fab.setOnClickListener {
			viewModel.saveSubscription()
		}
		
		binding.fieldName.setText(viewModel.subscription.name)
		binding.fieldName.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(name = it?.toString() ?: "")
			if (viewModel.canSave) binding.fab.show() else binding.fab.hide()
		}
		
		binding.fieldDescription.setText(viewModel.subscription.description)
		binding.fieldDescription.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(description = it?.toString() ?: "")
		}
		
		
		if (viewModel.subscription.price > 0.0)
			binding.fieldAmount.setText(viewModel.subscription.price.toString())
		binding.fieldAmount.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(price = it?.toString()?.toDoubleOrNull() ?: 0.0)
		}
		
		updateCurrency(viewModel.subscription.currency)
		binding.buttonCurrency.setOnClickListener {
			CurrencyPickerSheet(this) {
				viewModel.subscription = viewModel.subscription.copy(currency = it)
				updateCurrency(it)
			}.show()
		}
		
		updateDate(viewModel.subscription.startedOn)
		binding.rowStartedOn.setOnClickListener {
			showDatePicker(viewModel.subscription.startedOn) { date ->
				viewModel.subscription = viewModel.subscription.copy(
					startedOn = date
				)
				updateDate(date)
				if (viewModel.canSave) binding.fab.show() else binding.fab.hide()
			}
		}
		
		updateFrequency(viewModel.subscription.period)
		binding.rowFrequency.setOnClickListener {
			FrequencyPickerSheet(this, viewModel.subscription.period) {
				viewModel.subscription = viewModel.subscription.copy(period = it)
				updateFrequency(it)
			}.show()
		}
		
		binding.fieldPaymentMethod.setText(viewModel.subscription.paymentMethod)
		binding.fieldPaymentMethod.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(paymentMethod = it?.toString() ?: "")
		}
		
		binding.fieldNotes.setText(viewModel.subscription.notes)
		binding.fieldNotes.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(notes = it?.toString() ?: "")
		}
		
		
		binding.cardColor.strokeColor = materialColor(R.attr.colorThemeInverse).alpha(0.12f)
		viewModel.subscription.color?.let {
			binding.cardColor.backgroundTintList = ColorStateList.valueOf(it)
		}
		binding.cardColor.setOnClickListener {

			showColorPickerDialog(
				viewModel.subscription.color
					?: IntegerHSLColor.createRandomColor().toColorInt()
			) { color ->
				viewModel.subscription = viewModel.subscription.copy(color = color)
				binding.cardColor.backgroundTintList = ColorStateList.valueOf(color)
			}
			
		}
	}
	
	private fun updateFrequency(period: BillingPeriod) {
		binding.textFrequency.text = period.stringId?.let { getString(it) }
	}
	
	private fun updateCurrency(currency: Currency) {
		binding.textCurrencySign.visible = currency.symbol != null
		binding.textCurrencySign.text = currency.symbol
		
		binding.buttonCurrency.text = currency.iso4217Alpha
	}
	
	private fun updateDate(date: FullDate?) {
		when (date) {
			null -> binding.textStartedOn.text = ""
			else -> binding.textStartedOn.text = DateTimeFormatter
				.ofLocalizedDate(FormatStyle.MEDIUM)
				.format(LocalDate.of(date.year, date.month, date.day))
		}
	}
	
	
	private fun showDatePicker(
		selectedDate: FullDate?,
		callback: (FullDate) -> Unit,
	) {
		val dateToUse =
			selectedDate?.toLong() ?: MaterialDatePicker.todayInUtcMilliseconds()
		
		MaterialDatePicker.Builder.datePicker()
			// TODO string res
			.setTitleText("Select date")
			.setSelection(dateToUse)
			.build()
			.apply {
				addOnPositiveButtonClickListener {
					val date = FullDate.from(it)
					
					callback(date)
				}
			}
			.show(supportFragmentManager, null)
	}
	
	private fun finishWithMessage() {
		setResult(5, Intent().apply {
			putExtra("Message", "Draft saved")
		})
		this.finish()
	}
	
}