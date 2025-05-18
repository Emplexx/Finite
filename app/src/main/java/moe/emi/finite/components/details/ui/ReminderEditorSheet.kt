package moe.emi.finite.components.details.ui

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import convenience.resources.Token
import convenience.resources.colorAttr
import convenience.resources.drawable
import convenience.resources.easingAttr
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import moe.emi.finite.R
import moe.emi.finite.core.model.Timespan
import moe.emi.finite.databinding.LayoutSheetReminderEditorBinding
import moe.emi.finite.dump.collectOn
import moe.emi.finite.core.model.Reminder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.max

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
		
		binding.headerDate.text.text = getString(R.string.reminders_remind_me)
		
		binding.rowTime.apply {
			layoutIcon.isVisible = true
			icon.setImageDrawable(requireActivity().drawable(R.drawable.ic_schedule_24))
			backgroundColor.setCardBackgroundColor(requireActivity().getColor(R.color.blue))
			textLabel.text = getString(R.string.reminders_time)
			textValue.isVisible = true
		}
		
		binding.footerDateError.root.isVisible = false
		binding.footerDateError.text.alpha = 1f
		binding.footerDateError.text.text = getString(R.string.reminders_error_period_too_long)
		binding.footerDateError.text.setTextColor(requireContext().colorAttr(Token.color.error))
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
			
			if (editable?.trim()?.startsWith("0") == true) {
				binding.fieldPeriodCount.text = null
				return@doAfterTextChanged
			}
			
			editable?.toString()?.toIntOrNull()
				.let { it ?: 1 }
				.let { max(1, it) }
				.let { viewModel.period = viewModel.period.copy(length = it) }
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
						viewModel.period = viewModel.period.copy(unit = it)
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
			binding.fieldPeriod.text = period.unit.name
			
			binding.fieldPeriodCount.let {
				val current = it.text?.toString() ?: ""
				val new = period.length.toString()
				
				val erasedToBlank = current.isBlank() && new == "1"
				if (!erasedToBlank && current != new) binding.fieldPeriodCount.setText(new)
			}
		}
		
		viewModel.isPeriodValid.distinctUntilChanged()
			.drop(1)
			.collectOn(viewLifecycleOwner) { isValid ->
				
				val root = binding.root.parent as? ViewGroup
				if (root != null) TransitionManager.beginDelayedTransition(root, transitionSet())
				
				binding.footerDateError.root.isVisible = !isValid
				binding.buttonSave.isEnabled = isValid
			}
	}
	
	private fun transitionSet() = TransitionSet().apply {
		addTransition(fadeTransition(Fade.OUT))
		addTransition(fadeTransition(Fade.IN))
		addTransition(ChangeBounds().apply {
			duration = 500
			interpolator = easingAttr(Token.easing.emphasizedDecelerated)
		})
		ordering = TransitionSet.ORDERING_TOGETHER
	}
	
	private fun fadeTransition(mode: Int) = Fade(mode).apply {
		duration = 250
		interpolator = easingAttr(Token.easing.standard)
	}
}