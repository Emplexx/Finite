package moe.emi.finite.ui.details

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dev.chrisbanes.insetter.applyInsetter
import moe.emi.convenience.TonalColor
import moe.emi.convenience.drawable
import moe.emi.convenience.materialColor
import moe.emi.finite.R
import moe.emi.finite.databinding.LayoutSheetReminderEditorBinding
import moe.emi.finite.dump.collectOn
import moe.emi.finite.service.model.Reminder
import moe.emi.finite.service.model.Timespan
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ReminderEditorSheet : BottomSheetDialogFragment() {
	
	private val viewModel by viewModels<ReminderEditorViewModel> { ReminderEditorViewModel }
	private lateinit var binding: LayoutSheetReminderEditorBinding
	
	private val behavior: BottomSheetBehavior<*>
		get() = (dialog as BottomSheetDialog).behavior
	
	companion object {
		fun newInstance(
			subscriptionId: Int,
			reminder: Reminder? = null
		) = ReminderEditorSheet().apply {
			arguments = Bundle().apply {
				putInt("ID", subscriptionId)
				reminder?.let { putSerializable("reminder", it) }
			}
		}
	}
	
	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = LayoutSheetReminderEditorBinding.inflate(layoutInflater)
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true, ime = true) {
				padding(bottom = true)
			}
		}
		
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
		behavior.skipCollapsed = true
		
		initLayout()
		initListeners()
		
		collectFlow()
	}
	
	private fun initLayout() {
		
		binding.headerDate.text.text = "Remind me"
		
		binding.rowTime.apply {
			layoutIcon.isVisible = true
			icon.setImageDrawable(requireActivity().drawable(R.drawable.ic_palette_fill_24))
			backgroundColor.setCardBackgroundColor(requireActivity().getColor(R.color.pink))
			textLabel.text = "Time"
			textValue.isVisible = true
		}
		
		binding.footerDateError.root.isVisible = false
		binding.footerDateError.text.alpha = 1f
		binding.footerDateError.text.text = "Reminder period should be shorter than a single billing cycle"
		binding.footerDateError.text.setTextColor(requireContext().materialColor(TonalColor.error))
	}
	
	private fun initListeners() {
		
		binding.rowSameDay.setOnClickListener {
			viewModel.sameDay = true
		}
		binding.rowPrior.setOnClickListener {
			viewModel.sameDay = false
		}

		binding.radioSameDay.setOnClickListener {
			viewModel.sameDay = true
		}
		binding.radioPrior.setOnClickListener {
			viewModel.sameDay = false
		}
		
		binding.fieldPeriodCount.doAfterTextChanged { editable ->
			editable?.toString()?.toIntOrNull()
				.let { it ?: 1 }
				.let { viewModel.period = viewModel.period.copy(count = it) }
		}
		
		binding.cardPeriod.setOnClickListener {
			PopupMenu(context, binding.cardPeriod).apply {
				menu.also {
					it.add(0, 0, 0, "Days")
					it.add(0, 1, 0, "Weeks")
					it.add(0, 2, 0, "Months")
					it.add(0, 3, 0, "Years")
				}
				setOnMenuItemClickListener { item ->
					when (item.itemId) {
						0 -> Timespan.Day
						1 -> Timespan.Week
						2 -> Timespan.Month
						3 -> Timespan.Year
						else -> null
					}?.let {
						viewModel.period = viewModel.period.copy(timespan = it)
					}
					true
				}
			}.show()
		}
		
		binding.rowTime.root.setOnClickListener {
			val picker =
				MaterialTimePicker.Builder()
					.setTimeFormat(if (DateFormat.is24HourFormat(requireContext()))
						TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
					.setHour(viewModel.reminder.hours)
					.setMinute(viewModel.reminder.minutes)
					.setTitleText("Select reminder time")
					.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
					.build()
			
			picker.show(parentFragmentManager, null)
			picker.addOnPositiveButtonClickListener {
				viewModel.reminder = viewModel.reminder.copy(
					hours = picker.hour,
					minutes = picker.minute
				)
			}
		}
		
		binding.buttonSave.setOnClickListener {
			binding.buttonSave.isEnabled = false
			viewModel.saveReminder { dismiss() }
		}
		
	}
	
	private fun collectFlow() {
		viewModel.sameDayFlow.collectOn(viewLifecycleOwner) {
			binding.radioSameDay.isChecked = it
			binding.radioPrior.isChecked = !it
		}
		viewModel.reminderFlow.collectOn(viewLifecycleOwner) {
			val c = LocalTime.of(it.hours, it.minutes)
			binding.rowTime.textValue.text = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(c)
		}
		viewModel.periodFlow.collectOn(viewLifecycleOwner) { period ->
			binding.fieldPeriod.text = period.timespan.name
			
			binding.fieldPeriodCount.let {
				val current = it.text?.toString() ?: ""
				val new = period.count.toString()
				
				val erasedToBlank = current.isBlank() && new == "1"
				if (!erasedToBlank && current != new) binding.fieldPeriodCount.setText(new)
			}
		}
		viewModel.isPeriodValid.collectOn(viewLifecycleOwner) { isValid ->
			binding.footerDateError.root.isVisible = !isValid
			binding.buttonSave.isEnabled = isValid
		}
	}
}