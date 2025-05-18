package moe.emi.finite.ui.editor

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isEmpty
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import codes.side.andcolorpicker.converter.toColorInt
import codes.side.andcolorpicker.model.IntegerHSLColor
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import convenience.resources.colorAttr
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking
import moe.emi.finite.R
import moe.emi.finite.components.currency.CurrencyPickerSheet
import moe.emi.finite.components.editor.ui.FrequencyPickerSheet
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Period
import moe.emi.finite.core.ui.format.formatStringId
import moe.emi.finite.databinding.ActivitySubscriptionEditorBinding
import moe.emi.finite.dump.alpha
import moe.emi.finite.dump.android.HasSnackbarAnchor
import moe.emi.finite.dump.android.snackbar
import moe.emi.finite.dump.collectOn
import moe.emi.finite.core.model.SimpleDate
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.min

class SubscriptionEditorActivity : AppCompatActivity(), HasSnackbarAnchor {
	
	private val viewModel by viewModels<SubscriptionEditorViewModel> { SubscriptionEditorViewModel }
	private lateinit var binding: ActivitySubscriptionEditorBinding
	
	private val callback = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			if (viewModel.subscription.id == 0) {
				runBlocking {
//					setDraft(viewModel.subscription)
					viewModel.saveDraft()
					Toast.makeText(this@SubscriptionEditorActivity, "Draft saved", Toast.LENGTH_SHORT).show()
					finish()
					// TODO message "draft saved"
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
		collectFlow()
	}
	
	private fun initViews() {
		binding.toolbar.setNavigationOnClickListener {
			onBackPressedDispatcher.onBackPressed()
		}
		binding.toolbar.setOnMenuItemClickListener { item ->
			if (item.itemId == R.id.action_clear_draft) viewModel.discardDraft()
			true
		}
		
		binding.fieldAmount.doAfterTextChanged {
			
			viewModel.subscription =
				viewModel.subscription.copy(price = it?.toString()?.toDoubleOrNull() ?: 0.0)
		}
		binding.buttonCurrency.setOnClickListener {
			CurrencyPickerSheet(
				this,
				true,
				onSelect = {
					viewModel.subscription = viewModel.subscription.copy(currency = it)
				}
			).show()
		}
		binding.cardColor.setOnClickListener {
			
			showColorPickerDialog(
				viewModel.subscription.color ?: IntegerHSLColor.createRandomColor().toColorInt(),
				onSelect = { color ->
					viewModel.subscription = viewModel.subscription.copy(color = color)
				}
			)
			
		}
		
		binding.fieldName.doAfterTextChanged {
			viewModel.subscription = viewModel.subscription.copy(name = it?.toString().orEmpty())
		}
		binding.fieldDescription.doAfterTextChanged {
			viewModel.subscription = viewModel.subscription.copy(description = it?.toString().orEmpty())
		}
		
		binding.rowStartedOn.setOnClickListener {
			showDatePicker(
				viewModel.subscription.startedOn,
				onSelect = { date ->
					viewModel.subscription = viewModel.subscription.copy(
						startedOn = date
					)
				}
			)
		}
		binding.rowFrequency.setOnClickListener {
			FrequencyPickerSheet(
				this,
				viewModel.subscription.period,
				onSelect = {
					viewModel.subscription = viewModel.subscription.copy(period = it)
				}
			).show()
		}
		
		binding.fieldPaymentMethod.doAfterTextChanged {
			viewModel.subscription = viewModel.subscription.copy(paymentMethod = it?.toString().orEmpty())
		}
		binding.fieldNotes.doAfterTextChanged {
			viewModel.subscription = viewModel.subscription.copy(notes = it?.toString().orEmpty())
		}
		
		binding.fab.setOnClickListener {
			viewModel.saveSubscription()
			binding.toolbar.menu.clear()
			binding.root.snackbar("Changes saved")
		}
		
		binding.cardColor.strokeColor = colorAttr(R.attr.colorThemeInverse).alpha(0.12f)
	}
	
	private fun collectFlow() {
		viewModel.showDraftIcon.collectOn(this) {
			val noMenu = binding.toolbar.menu.isEmpty()
			if (it && noMenu) binding.toolbar.inflateMenu(R.menu.menu_editor)
			if (!it) binding.toolbar.menu.clear()
		}
		
		viewModel.canSaveFlow.collectOn(this) {
			if (it) binding.fab.show() else binding.fab.hide()
			binding.textRequiredFieldsHint.isVisible = !it
		}
		viewModel.subscriptionFlow.filterNotNull().collectOn(this) { subscription ->
			
			if (subscription.price > 0.0) run {
				
				val format = DecimalFormat("0.##")
					.apply { roundingMode = RoundingMode.DOWN }
					.format(viewModel.subscription.price)
				
				val whole = format.substringBefore('.')
				
				val old = binding.fieldAmount.text?.toString() ?: ""
				val oldWhole = old.substringBefore('.')
				
				if (whole != oldWhole) return@run binding.fieldAmount.updateText(format)
				
				val endsInDot = old.lastOrNull() == '.'
				val endsInZeroes = old.substringAfter('.', "no zeroes here").all { it == '0' }
				
				if (!endsInDot && !endsInZeroes) binding.fieldAmount.updateText(format)
				
			}
			else binding.fieldAmount.updateText("")
			
			binding.fieldName.updateText(subscription.name)
			binding.fieldDescription.updateText(subscription.description)
			binding.fieldPaymentMethod.updateText(subscription.paymentMethod)
			binding.fieldNotes.updateText(subscription.notes)
			
			updateCurrency(subscription.currency)
			updateDate(subscription.startedOn)
			updateFrequency(subscription.period)
			subscription.color?.let {
				binding.cardColor.backgroundTintList = ColorStateList.valueOf(it)
			}
		}
	}
	
	private fun updateFrequency(period: Period) {
		binding.textFrequency.text = period.formatStringId?.let { getString(it) }
	}
	
	private fun updateCurrency(currency: Currency) {
		binding.textCurrencySign.isVisible = currency.symbol != null
		binding.textCurrencySign.text = currency.symbol
		binding.buttonCurrency.text = currency.iso4217Alpha
	}
	
	private fun updateDate(date: SimpleDate?) {
		when (date) {
			null -> binding.textStartedOn.text = ""
			else -> binding.textStartedOn.text = DateTimeFormatter
				.ofLocalizedDate(FormatStyle.MEDIUM)
				.format(LocalDate.of(date.year, date.month, date.day))
		}
	}
	
	
	private fun showDatePicker(
		selectedDate: SimpleDate?,
		onSelect: (SimpleDate) -> Unit,
	) {
		val dateToUse = selectedDate?.toUtcMilliseconds() ?: MaterialDatePicker.todayInUtcMilliseconds()
		
		MaterialDatePicker.Builder.datePicker()
			.setTitleText("Select date") // TODO string res
			.setSelection(dateToUse)
			.build()
			.apply {
				addOnPositiveButtonClickListener {
					val date = SimpleDate.fromUtcMilliseconds(it)
					onSelect(date)
				}
			}
			.show(supportFragmentManager, null)
	}
	
	private fun showUnsavedChangesDialog() {
		MaterialAlertDialogBuilder(this)
			.setTitle("Discard changes?")
			.setMessage("You have made changes to this item that have not been saved")
			.setPositiveButton("Discard") { _, _ -> finish() }
			.setNegativeButton("Cancel", null)
			.show()
	}
	
	
	private fun finish(withMessage: Boolean) {
		if (withMessage) setResult(5, Intent().apply {
			putExtra("Message", "Draft saved")
		})
		this.finish()
	}
	
	private fun EditText.updateText(newText: CharSequence) {
		val oldText: CharSequence = this.text?.toString() ?: ""
		val oldSelection = this.selectionStart
		if (oldText != newText) {
			setText(newText)
			if (this.hasFocus()) {
				setSelection(min(oldSelection, this.editableText?.length ?: 0))
			}
		}
	}
	
	override val snackbarAnchor: View
		get() = binding.fab
	
}