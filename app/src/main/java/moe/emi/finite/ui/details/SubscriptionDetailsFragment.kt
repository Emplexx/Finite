package moe.emi.finite.ui.details

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
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
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.convenience.materialColor
import moe.emi.finite.MainActivity
import moe.emi.finite.MainViewModel
import moe.emi.finite.R
import moe.emi.finite.databinding.FragmentSubscriptionDetailsBinding
import moe.emi.finite.dump.isStatusBarLightTheme
import moe.emi.finite.dump.setStatusBarThemeMatchSystem
import moe.emi.finite.dump.snackbar
import moe.emi.finite.dump.visible
import moe.emi.finite.service.data.convert
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.ui.colors.ItemColors
import moe.emi.finite.ui.colors.PaletteTone
import moe.emi.finite.ui.colors.makeItemColors
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import java.math.RoundingMode
import java.text.DecimalFormat
import com.google.android.material.R as GR

class SubscriptionDetailsFragment : Fragment() {
	
	private val mainViewModel by activityViewModels<MainViewModel>()
	private val viewModel by viewModels<SubscriptionDetailsViewModel>()
	lateinit var binding: FragmentSubscriptionDetailsBinding
	
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
		
		binding.toolbar.setNavigationOnClickListener {
			requireActivity().onBackPressedDispatcher.onBackPressed()
		}
		binding.toolbar.setOnMenuItemClickListener {
			when (it.itemId) {
				R.id.action_pause -> {
					viewModel.pauseSubscription()
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
	
}