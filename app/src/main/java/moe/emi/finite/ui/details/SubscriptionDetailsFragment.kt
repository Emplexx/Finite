package moe.emi.finite.ui.details

import android.app.AlarmManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.content.getSystemService
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.transition.Slide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.motion.MotionUtils
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dagger.hilt.android.AndroidEntryPoint
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import moe.emi.convenience.materialColor
import moe.emi.finite.JavaSerializable
import moe.emi.finite.MainActivity
import moe.emi.finite.MainViewModel
import moe.emi.finite.R
import moe.emi.finite.databinding.FragmentSubscriptionDetailsBinding
import moe.emi.finite.dump.isStatusBarLightTheme
import moe.emi.finite.dump.setStatusBarThemeMatchSystem
import moe.emi.finite.dump.snackbar
import moe.emi.finite.dump.visible
import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.Subscription.Companion.findNextPaymentInclusive
import moe.emi.finite.service.data.Subscription.Companion.plus
import moe.emi.finite.service.data.Timespan
import moe.emi.finite.service.data.convert
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.notifications.AlarmScheduler
import moe.emi.finite.ui.colors.ItemColors
import moe.emi.finite.ui.colors.PaletteTone
import moe.emi.finite.ui.colors.makeItemColors
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import java.math.RoundingMode
import java.text.DecimalFormat
import java.time.Period
import com.google.android.material.R as GR

@AndroidEntryPoint
class SubscriptionDetailsFragment : Fragment() {
	
	private val mainViewModel by activityViewModels<MainViewModel>()
	private val viewModel by viewModels<SubscriptionDetailsViewModel>()
	lateinit var binding: FragmentSubscriptionDetailsBinding
	
	val alarmScheduler by lazy { AlarmScheduler(requireContext()) }
	
	private val activity: MainActivity
		get() = requireActivity() as MainActivity
	
	private lateinit var colors: ItemColors
	private val appBarListener by lazy { object : AppBarChangeColorListener(binding.toolbar) {
		
		override fun getCollapsedColor(): Int {
			return requireActivity().materialColor(GR.attr.colorControlNormal)
		}
		
		override fun getExpandedColor(): Int {
			return colors.onContainerVariant
		}
		
		override fun onExpandCallback() {
			requireActivity().isStatusBarLightTheme =
				colors.tone.getMatchingStatusBarColor(requireContext())
		}
		
		override fun onCollapseCallback() {
			requireActivity().setStatusBarThemeMatchSystem()
		}
	} }
	
	lateinit var adapterReminders: GroupieAdapter
	lateinit var adapterUpcoming: GroupieAdapter
	val sectionReminders by lazy { Section() }
	val sectionUpcoming by lazy { Section() }
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sharedElementEnterTransition = MaterialContainerTransform().apply {
			drawingViewId = R.id.nav_host_fragment_content_main
			scrimColor = Color.TRANSPARENT
		}
		returnTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentSubscriptionDetailsBinding.inflate(inflater, container, false)
		return binding.root
	}
	
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		colors = ItemColors(requireContext())
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		
		adapterReminders = GroupieAdapter()
		adapterReminders.add(sectionReminders)
		binding.recyclerViewReminders.adapter = adapterReminders
		adapterUpcoming = GroupieAdapter()
		adapterUpcoming.add(sectionUpcoming)
		binding.recyclerViewUpcoming.adapter = adapterUpcoming
		
		
		binding.toolbar.setNavigationOnClickListener {
			requireActivity().onBackPressedDispatcher.onBackPressed()
		}
		binding.toolbar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_pause -> {
					viewModel.pauseSubscription()
					true
				}
				R.id.action_notification -> {
					
					if (requireContext().getSystemService<AlarmManager>()!!.canScheduleExactAlarms()) {
						createNotification()
					} else {
						
						MaterialAlertDialogBuilder(requireActivity())
							.setTitle("Permission required")
							.setMessage("Finite requires a permission to schedule notifications, which you can grant in settings")
							.setPositiveButton("Take me there") { _, _ ->
								
								Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
									Uri.parse("package:" + requireContext().packageName))
									.let(requireActivity()::startActivity)
							}
							.setNegativeButton("Cancel", null)
							.show()
					}
					
					true
				}
				R.id.action_edit -> {
					viewModel.subscription.value?.let {
						startActivity(Intent(requireActivity(), SubscriptionEditorActivity::class.java)
								.putExtra("Subscription", it))
					}
					true
				}
				R.id.action_delete -> {
					MaterialAlertDialogBuilder(requireContext())
						.setTitle("Delete subscription?")
						.setNegativeButton("Cancel", null)
						.setPositiveButton("Delete") { _, _ ->
							viewModel.deleteSubscription()
						}
						.show()
					
					true
				}
				else -> false
			}
		}
		binding.appBarLayout.addOnOffsetChangedListener(appBarListener)
		
		collect()
	}
	
	override fun onResume() {
		super.onResume()
		setMatchingStatusBarColor(colors.tone)
	}
	
	override fun onPause() {
		requireActivity().setStatusBarThemeMatchSystem()
		super.onPause()
	}
	
	override fun onDestroy() {
		requireActivity().setStatusBarThemeMatchSystem()
		super.onDestroy()
	}
	
	private fun collect() {
		viewModel.subscription.observe(viewLifecycleOwner) { model -> model ?: return@observe
			
			requireContext().makeItemColors(model.color).let {
				
				if (this.colors != it) {
					setMatchingStatusBarColor(it.tone)
					binding.toolbar.setNavigationIconTint(it.onContainerVariant)
					binding.toolbar.menu.forEach { item ->
						item.iconTintList = ColorStateList.valueOf(it.onContainerVariant)
					}
					binding.toolbar.overflowIcon?.setTint(it.onContainerVariant)
				}
				
				this.colors = it
				
				binding.layoutReceipt.setBackgroundColor(it.container)
				binding.receiptView.color = it.container
				binding.appBarLayout.setBackgroundColor(it.container)
				
				listOf(
					binding.textName,
					binding.textCurrencySign,
					binding.textAmount,
					binding.textDescription,
					binding.textConvertedAmount
				).forEach { v -> v.setTextColor(it.onContainer) }
			}
			
			val iconRes = if (model.active) R.drawable.ic_pause_circle_24
			else R.drawable.ic_play_circle_24
			val stringRes = if (model.active) "Pause subscription"
			else "Resume subscription"
			binding.toolbar.menu.findItem(R.id.action_pause)?.let {
				it.setIcon(iconRes)
				it.setTitle(stringRes)
			}
			
			lifecycleScope.launch {
				
				val defCurrency = appSettings.first().preferredCurrency
				
				binding.textName.text = model.name
				binding.textCurrencySign.text = model.currency.symbol ?: model.currency.iso4217Alpha
				binding.textAmount.text = DecimalFormat("0.00")
					.apply { roundingMode = RoundingMode.CEILING }
					.format(model.price)
				
				binding.textDescription.text = model.description
				binding.textConvertedAmount.visible =
					defCurrency != model.currency
				binding.textConvertedAmount.text = buildString {
					append("≈ ${defCurrency.symbol ?: defCurrency.iso4217Alpha} ")
					DecimalFormat("0.00")
						.apply { roundingMode = RoundingMode.CEILING }
						.format(convert(model.price, model.currency, defCurrency))
						.let(::append)
				}
			}
			
			binding.textNotes.visible = model.notes.isNotBlank()
			binding.textNotes.text = model.notes
			
			run upcoming@ {
				model.startedOn ?: return@upcoming
				
				val nextPayment = model.startedOn
					.toLocalDate()
					.findNextPaymentInclusive(model.period)
				val list = mutableListOf(nextPayment)
				repeat(12) {
					list += list.last().plus(model.period)
				}
				
				list.map {
					UpcomingPaymentAdapterItem(it, model.period)
				}.let(sectionUpcoming::update)
			}
		}
		
		viewModel.reminders.observe(viewLifecycleOwner) { reminders ->
			
			reminders.map { reminder ->
				ReminderAdapterItem(
					reminder,
					onEdit = {
						ReminderEditorSheet.newInstance(viewModel.entityId, reminder)
							.show(parentFragmentManager, null)
					},
					onRemove = {
						viewModel.deleteReminder(reminder.id)
					}
				)
			}.let(sectionReminders::update)
		}
		
		viewModel.events.observe(viewLifecycleOwner) { it ?: return@observe
			if (!it.consumed) when (it.key) {
				Event.Error -> binding.root.snackbar("Something went wrong")
				"Paused" -> binding.root.snackbar("Subscription paused")
				"Resumed" -> binding.root.snackbar("Subscription resumed")
				Event.Delete -> {
					
					sharedElementEnterTransition = null
					returnTransition = Slide().apply {
						interpolator = MotionUtils.resolveThemeInterpolator(requireContext(),
							GR.attr.motionEasingStandardAccelerateInterpolator, LinearInterpolator())
						duration = 250
					}
					
					activity.onBackPressedDispatcher.onBackPressed()
					mainViewModel.messages.postValue(Event(Event.Delete))
				}
			}
			it.consume()
		}
	}
	
	fun setMatchingStatusBarColor(tone: PaletteTone) {
		if (appBarListener.lastAnimatedState == AppBarStateChangeListener.State.COLLAPSED) {
			requireActivity().setStatusBarThemeMatchSystem()
		} else {
			requireActivity().isStatusBarLightTheme = tone.getMatchingStatusBarColor(requireContext())
		}
	}
	
	fun createNotification() {

		ReminderEditorSheet.newInstance(viewModel.entityId)
			.show(parentFragmentManager, null)
		
//		lifecycleScope.launch {
//
//			val (hour, minute) = Calendar.getInstance().let {
//				it.add(Calendar.MINUTE, 1)
//				it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
//			}
//
//			Log.d("TAG", "$hour : $minute")
//
//			NotificationRepo.dao.insertAll(
//				NotificationEntity(
//					Reminder(
//						0,
//						viewModel.subscription.value?.id!!,
//						null,
//						hour, minute
//					)
//				)
//
//			).let {
//
//
//				Log.d("TAG", "$it")
//
//				alarmScheduler.scheduleAlarms(it.first().toInt())
//			}
//
//		}
	
	
	}
	
}


// TODO merge this with BillingPeriod because they're the same thing
@Serializable
data class NotificationPeriod(
	val count: Int,
	val timespan: Timespan
) : JavaSerializable {
	
	companion object {
		fun BillingPeriod.toNotificationPeriod() =
			NotificationPeriod(count, timespan)
	}
	
	private val approximateLength: Int
		get() = this.timespan.approximateMultiplier * this.count
	
	operator fun compareTo(other: BillingPeriod): Int {
		return this.approximateLength - other.toNotificationPeriod().approximateLength
	}
	
	fun toJavaPeriod() =
		when (timespan) {
			Timespan.Day -> Period.ofDays(count)
			Timespan.Week -> Period.ofWeeks(count)
			Timespan.Month -> Period.ofMonths(count)
			Timespan.Year -> Period.ofYears(count)
		}
	
}