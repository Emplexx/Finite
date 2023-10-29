package moe.emi.finite.ui.details

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import moe.emi.convenience.TonalColor
import moe.emi.convenience.drawable
import moe.emi.convenience.materialColor
import moe.emi.finite.R
import moe.emi.finite.databinding.LayoutSheetReminderEditorBinding
import moe.emi.finite.dump.visible
import moe.emi.finite.service.data.Reminder
import moe.emi.finite.service.data.Timespan
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@AndroidEntryPoint
class ReminderEditorSheet() : BottomSheetDialogFragment() {
	
	private val viewModel by viewModels<ReminderEditorViewModel>()
	private lateinit var binding: LayoutSheetReminderEditorBinding
	
	private val behavior: BottomSheetBehavior<*>
		get() = (dialog as BottomSheetDialog).behavior
	
	companion object {
		fun newInstance(entityId: Int, reminder: Reminder? = null) = ReminderEditorSheet().apply {
			arguments = Bundle().apply {
				putInt("ID", entityId)
				reminder?.let { putSerializable("Reminder", it) }
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
		
		lifecycleScope.launch {
			initLayout()
			initListeners()
		}
	}
	
	private fun initLayout() {
		
		binding.headerDate.text.text = "Remind me"
		
		binding.footerDateError.root.visible = false
		binding.footerDateError.text.alpha = 1f
		binding.footerDateError.text.text = "Reminder period should be shorter than a single billing cycle"
		binding.footerDateError.text.setTextColor(requireContext().materialColor(TonalColor.error))
		
		when (viewModel.reminder.period) {
			null -> {
				binding.radioSameDay.isChecked = true
			}
			else -> {
				binding.radioPrior.isChecked = true
				binding.fieldPeriodCount.setText(viewModel.reminder.period?.count.toString())
				binding.fieldPeriod.text = viewModel.reminder.period?.timespan?.name
			}
		}
		
		binding.rowTime.apply {
			layoutIcon.visible = true
			icon.setImageDrawable(requireActivity().drawable(R.drawable.ic_palette_fill_24))
			backgroundColor.setCardBackgroundColor(requireActivity().getColor(R.color.pink))
			textLabel.text = "Time"
			updateTime()
			textValue.visible = true
		}
	}
	
	private suspend fun initListeners() {
		
		binding.rowSameDay.setOnClickListener {
			binding.radioSameDay.isChecked = true
		}
		binding.rowPrior.setOnClickListener {
			binding.radioPrior.isChecked = true
		}
		
		binding.radioSameDay.setOnCheckedChangeListener { _, isChecked ->
			binding.radioPrior.isChecked = !isChecked
			viewModel.reminder = viewModel.reminder.copy(period = null)
		}
		binding.radioPrior.setOnCheckedChangeListener { _, isChecked ->
			binding.radioSameDay.isChecked = !isChecked
			viewModel.reminder = viewModel.reminder.copy(period = viewModel.period)
		}
		
		binding.fieldPeriodCount.doAfterTextChanged { editable ->
			editable?.toString()?.toIntOrNull()
				.let { it ?: 1 }
				.let {
					viewModel.period = viewModel.period.copy(count = it)
					if (viewModel.reminder.period != null)
						viewModel.reminder = viewModel.reminder.copy(period = viewModel.period)
				}
			binding.footerDateError.root.visible = !viewModel.isPeriodValid
			binding.buttonSave.isEnabled = viewModel.isPeriodValid
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
						if (viewModel.reminder.period != null)
							viewModel.reminder = viewModel.reminder.copy(period = viewModel.period)
						binding.fieldPeriod.text = it.name
						
						binding.footerDateError.root.visible = !viewModel.isPeriodValid
						binding.buttonSave.isEnabled = viewModel.isPeriodValid
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
				updateTime()
			}
		}
		
		binding.buttonSave.setOnClickListener {
			binding.buttonSave.isEnabled = false
			viewModel.saveReminder { dismiss() }
		}
		
	}
	
	private fun updateTime() {
		val c = LocalTime.of(viewModel.reminder.hours, viewModel.reminder.minutes)
		binding.rowTime.textValue.text = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(c)
	}
}