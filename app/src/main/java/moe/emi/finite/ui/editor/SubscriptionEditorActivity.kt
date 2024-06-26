package moe.emi.finite.ui.editor

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import codes.side.andcolorpicker.converter.toColorInt
import codes.side.andcolorpicker.model.IntegerHSLColor
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import moe.emi.convenience.materialColor
import moe.emi.finite.R
import moe.emi.finite.databinding.ActivitySubscriptionEditorBinding
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.alpha
import moe.emi.finite.dump.snackbar
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.setDraft
import moe.emi.finite.service.model.BillingPeriod
import moe.emi.finite.service.model.Currency
import moe.emi.finite.service.model.FullDate
import moe.emi.finite.ui.currency.CurrencyPickerSheet
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class SubscriptionEditorActivity : AppCompatActivity(), HasSnackbarAnchor {
	
	private val viewModel by viewModels<SubscriptionEditorViewModel>()
	private lateinit var binding: ActivitySubscriptionEditorBinding
	
	private val callback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			if (viewModel.subscription.id == 0) {
				lifecycleScope.launch {
					setDraft(viewModel.subscription)
					finish(viewModel.hasUnsavedChanges)
				}
			} else {
				if (viewModel.hasUnsavedChanges) {
					showUnsavedChangesDialog()
				} else {
					this.isEnabled = false
					onBackPressedDispatcher.onBackPressed()
				}
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
		binding.toolbar.setOnMenuItemClickListener { item ->
			when (item.itemId) {
				R.id.action_clear_draft -> {
					lifecycleScope.launch {
						viewModel.clearDraft()
						binding.toolbar.menu.clear()
						initViews()
					}
					true
				}
				else -> false
			}
		}
		lifecycleScope.launch {
			if (viewModel.isDraft()) {
				binding.toolbar.inflateMenu(R.menu.menu_editor)
			}
		}
		
		binding.fab.visible = viewModel.canSave
		binding.textRequiredFieldsHint.visible = !viewModel.canSave
		binding.fab.setOnClickListener {
			viewModel.saveSubscription()
			binding.toolbar.menu.clear()
			binding.root.snackbar("Changes saved")
		}
		
		binding.fieldName.setText(viewModel.subscription.name)
		binding.fieldName.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(name = it?.toString() ?: "")
			if (viewModel.canSave) {
				binding.textRequiredFieldsHint.visible = false
				binding.fab.show()
			} else {
				binding.textRequiredFieldsHint.visible = true
				binding.fab.hide()
			}
		}
		
		binding.fieldDescription.setText(viewModel.subscription.description)
		binding.fieldDescription.doAfterTextChanged {
			viewModel.subscription =
				viewModel.subscription.copy(description = it?.toString() ?: "")
		}
		
		
		if (viewModel.subscription.price > 0.0)
			binding.fieldAmount.setText(
				DecimalFormat("0.##")
				.apply { roundingMode = RoundingMode.CEILING }
				.format(viewModel.subscription.price))
		else binding.fieldAmount.setText("")
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
				if (viewModel.canSave) {
					binding.textRequiredFieldsHint.visible = false
					binding.fab.show()
				} else {
					binding.textRequiredFieldsHint.visible = true
					binding.fab.hide()
				}
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
	
	private fun showUnsavedChangesDialog() {
		MaterialAlertDialogBuilder(this)
			.setTitle("Discard changes?")
			.setMessage("You have made changes to this item that have not been saved")
			.setPositiveButton("Discard") { _, _ ->
				finish()
			}
			.setNegativeButton("Cancel", null)
			.show()
	}
	
	
	private fun finish(withMessage: Boolean) {
		if (withMessage) setResult(5, Intent().apply {
			putExtra("Message", "Draft saved")
		})
		this.finish()
	}
	
	override val snackbarAnchor: View
		get() = binding.fab
	
}