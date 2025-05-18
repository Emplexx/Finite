package moe.emi.finite.ui.details

import android.app.AlarmManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.RoundedCorner
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.content.getSystemService
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.ChangeBounds
import androidx.transition.Slide
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.motion.MotionUtils
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.transition.MaterialContainerTransform
import com.google.android.material.transition.MaterialSharedAxis
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import convenience.resources.Token
import convenience.resources.colorAttr
import convenience.resources.easingAttr
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import moe.emi.finite.MainActivity
import moe.emi.finite.MainViewModel
import moe.emi.finite.R
import moe.emi.finite.components.colors.ItemColors
import moe.emi.finite.components.colors.PaletteTone
import moe.emi.finite.components.colors.makeItemColors
import moe.emi.finite.components.details.calculateTimesCharged
import moe.emi.finite.components.details.ui.ReminderEditorSheet
import moe.emi.finite.components.details.ui.adapter.AddReminderAdapterItem
import moe.emi.finite.components.details.ui.adapter.ReminderAdapterItem
import moe.emi.finite.core.findNextPaymentInclusive
import moe.emi.finite.core.plus
import moe.emi.finite.core.ui.animator.SmoothItemAnimator
import moe.emi.finite.core.ui.format.formatPrice
import moe.emi.finite.databinding.FragmentSubscriptionDetailsBinding
import moe.emi.finite.dump.Event
import moe.emi.finite.dump.android.snackbar
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.isStatusBarLightTheme
import moe.emi.finite.dump.setStatusBarThemeMatchSystem
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import com.google.android.material.R as GR

class  SubscriptionDetailsFragment : Fragment() {
	
	private val mainViewModel by activityViewModels<MainViewModel>()
	private val viewModel by viewModels<SubscriptionDetailsViewModel> { SubscriptionDetailsViewModel }
	lateinit var binding: FragmentSubscriptionDetailsBinding
	
	private val activity: MainActivity get() = requireActivity() as MainActivity
	
	private lateinit var colors: ItemColors
	private val appBarListener by lazy {
		object : AppBarChangeColorListener(binding.toolbar) {
			
			override fun getCollapsedColor(): Int {
				return requireActivity().colorAttr(GR.attr.colorControlNormal)
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
		}
	}
	
	private lateinit var adapterReminders: GroupieAdapter
	private lateinit var adapterUpcoming: GroupieAdapter
	private val sectionReminders by lazy { Section() }
	private val itemAddReminder by lazy { AddReminderAdapterItem { tryCreateReminder() } }
	private val sectionUpcoming by lazy { Section() }
	
	val itemAnimator = SmoothItemAnimator()
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		sharedElementEnterTransition = MaterialContainerTransform().apply {
			drawingViewId = R.id.nav_host_fragment_content_main
			scrimColor = Color.TRANSPARENT
			
			shapeMaskProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
			
			endShapeAppearanceModel = ShapeAppearanceModel.builder()
				.setAllCornerSizes(run {
					requireActivity().window.decorView.rootWindowInsets
						?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
						?.radius
						?: 0
				}.toFloat())
				.build()
		}
		sharedElementReturnTransition = MaterialContainerTransform().apply {
			drawingViewId = R.id.nav_host_fragment_content_main
			scrimColor = Color.TRANSPARENT

			shapeMaskProgressThresholds = MaterialContainerTransform.ProgressThresholds(0f, 1f)
			
			startShapeAppearanceModel = ShapeAppearanceModel.builder()
				.setAllCornerSizes(run {
					requireActivity().window.decorView.rootWindowInsets
						?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
						?.radius
						?: 0
				}.toFloat())
				.build()
			
			endShapeAppearanceModel = ShapeAppearanceModel.builder()
				.setAllCornerSizes(8.fDp)
				.build()
			
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
		
		binding.toolbar.setNavigationOnClickListener {
			requireActivity().onBackPressedDispatcher.onBackPressed()
		}
		
		adapterReminders = GroupieAdapter()
		adapterReminders.add(sectionReminders)
		binding.recyclerViewReminders.adapter = adapterReminders
		binding.recyclerViewReminders.itemAnimator = itemAnimator
		
		adapterUpcoming = GroupieAdapter()
		adapterUpcoming.add(sectionUpcoming)
		binding.recyclerViewUpcoming.adapter = adapterUpcoming
		
		binding.toolbar.setOnMenuItemClickListener {
			when (it.itemId) {
				
				R.id.action_pause -> viewModel.togglePauseSubscription()
				
				R.id.action_edit -> viewModel.subscription.value?.let {
					startActivity(
						Intent(requireActivity(), SubscriptionEditorActivity::class.java)
							.putExtra("Subscription", it.model)
					)
				}
				
				R.id.action_delete -> MaterialAlertDialogBuilder(requireContext())
					.setTitle("Delete subscription?")
					.setNegativeButton("Cancel", null)
					.setPositiveButton("Delete") { _, _ ->
						viewModel.deleteSubscription()
					}
					.show()
			}
			
			true
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
		
		viewModel.subscription.filterNotNull().collectOn(viewLifecycleOwner) { uiModel ->
			
			val (model, defCurrency, convertedAmount, colorOptions) = uiModel
			
			// Header
			
			setColors(requireContext().makeItemColors(model.color, colorOptions))
			
			val iconRes =
				if (model.isActive) R.drawable.ic_pause_circle_24 else R.drawable.ic_play_circle_24
			val stringRes = if (model.isActive) "Pause subscription" else "Resume subscription"
			
			binding.toolbar.menu.findItem(R.id.action_pause)?.let {
				it.setIcon(iconRes)
				it.setTitle(stringRes)
			}
			
			binding.textName.text = model.name
			binding.textCurrencySign.text = model.currency.symbol ?: model.currency.iso4217Alpha
			binding.textAmount.text = formatPrice(model.price)
			
			binding.textDescription.text = model.description
			binding.textConvertedAmount.isVisible = convertedAmount != null
			
			if (convertedAmount != null) binding.textConvertedAmount.text = buildString {
				append("≈ ${defCurrency.symbol ?: defCurrency.iso4217Alpha} ")
				
				append(formatPrice(convertedAmount))
			}
			
			// Details
			
			run upcoming@{
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
			
			binding.sectionBillingPeriod.isVisible = model.startedOn != null
			binding.textBillingPeriod.text = "Every ${model.period.length} ${model.period.unit}"
			binding.textBillingStarted.text = "Started on ${model.startedOn?.toLocalDate()}"
			
			binding.sectionNotes.isVisible = model.notes.isNotBlank()
			binding.textNotes.text = model.notes
			
			model.startedOn?.toLocalDate()
				?.let { calculateTimesCharged(it, model.period) }
				?.let { model.price * it }
				?.let { formatPrice(it) }
				.let {
					binding.sectionPaidTotal.isVisible = it != null
					binding.textPaidTotal.text = it
				}
		}
		
		viewModel.reminders
			.map { it.size }
			.distinctUntilChanged()
			.drop(1)
//			.zipWithLast()
			.onEach { //(prev, next) ->
				
//				if (prev == null) return@onEach
//				val dur = if (prev > next) itemAnimator.moveDuration else 200
				
				TransitionManager.beginDelayedTransition(
					binding.root,
					ChangeBounds().apply {
						interpolator = easingAttr(Token.easing.emphasizedDecelerated)
						excludeChildren(binding.recyclerViewReminders, true)
						
						duration = itemAnimator.moveDuration
					}
				)
			}
			.collectOn(viewLifecycleOwner) {}
		
		viewModel.reminders.collectOn(viewLifecycleOwner) { reminders ->
			
			reminders
				.map { reminder ->
					ReminderAdapterItem(
						reminder,
						onEdit = {
							ReminderEditorSheet.newInstance(viewModel.entityId, reminder)
								.show(parentFragmentManager, null)
						},
						onRemove = {
							viewModel.onDeleteReminder(reminder.id)
						}
					)
				}
				.plus(itemAddReminder)
				.let(sectionReminders::update)
		}
		
		viewModel.events
			.filterNotNull()
			.filterNot { it.consumed }
			.collectOn(viewLifecycleOwner) {
				when (it.key) {
					Event.Error -> binding.root.snackbar("Something went wrong")
					"Paused" -> binding.root.snackbar("Subscription paused")
					"Resumed" -> binding.root.snackbar("Subscription resumed")
					Event.Deleted -> {
						
						sharedElementEnterTransition = null
						returnTransition = Slide().apply {
							interpolator = MotionUtils.resolveThemeInterpolator(
								requireContext(),
								GR.attr.motionEasingStandardAccelerateInterpolator,
								LinearInterpolator()
							)
							duration = 250
						}
						
						activity.onBackPressedDispatcher.onBackPressed()
						mainViewModel.messages.postValue(Event(Event.Deleted))
					}
				}
				it.consume()
			}
	}
	
	private fun setColors(it: ItemColors) {
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
	
	fun setMatchingStatusBarColor(tone: PaletteTone) {
		if (appBarListener.lastAnimatedState == AppBarStateChangeListener.State.COLLAPSED) {
			requireActivity().setStatusBarThemeMatchSystem()
		}
		else {
			requireActivity().isStatusBarLightTheme =
				tone.getMatchingStatusBarColor(requireContext())
		}
	}
	
	private fun tryCreateReminder() {
		
		// TODO show notifications permission ask
		if (requireContext().getSystemService<AlarmManager>()!!.canScheduleExactAlarms()) {
			ReminderEditorSheet.newInstance(viewModel.entityId).show(parentFragmentManager, null)
		}
		else {
			MaterialAlertDialogBuilder(requireActivity())
				.setTitle("Permission required")
				.setMessage("Finite requires a permission to schedule notifications, which you can grant in settings")
				.setPositiveButton("Take me there") { _, _ ->
					
					Intent(
						Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
						Uri.parse("package:" + requireContext().packageName)
					)
						.let(requireActivity()::startActivity)
				}
				.setNegativeButton("Cancel", null)
				.show()
		}
	}
	
}

