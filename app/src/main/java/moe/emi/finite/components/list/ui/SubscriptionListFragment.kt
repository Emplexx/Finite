package moe.emi.finite.components.list.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import arrow.core.partially2
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialSharedAxis
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.emi.finite.BuildConfig
import moe.emi.finite.FiniteApp
import moe.emi.finite.MainActivity
import moe.emi.finite.R
import moe.emi.finite.components.colors.makeItemColors
import moe.emi.finite.components.list.domain.SubscriptionItemUiModel
import moe.emi.finite.components.list.domain.SubscriptionListUiModel
import moe.emi.finite.components.list.domain.TotalView
import moe.emi.finite.components.list.ui.adapter.AutoLayoutDecoration
import moe.emi.finite.components.list.ui.adapter.BannerRemindersSuspendedItem
import moe.emi.finite.components.list.ui.adapter.HomeHeaderAdapterItem
import moe.emi.finite.components.list.ui.adapter.SubscriptionAdapterItem
import moe.emi.finite.components.list.ui.adapter.java.ExpandableHeaderItem
import moe.emi.finite.components.list.ui.adapter.java.ExpandableSection
import moe.emi.finite.components.settings.store.ColorOptions
import moe.emi.finite.components.upgrade.UpgradeSheet
import moe.emi.finite.core.ui.format.formatPrice
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.ui.animator.SmoothItemAnimator
import moe.emi.finite.databinding.FragmentSubscriptionsListBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.android.snackbar
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.iDp
import moe.emi.finite.core.model.Subscription
import moe.emi.finite.ui.editor.SubscriptionEditorActivity

class SubscriptionListFragment : Fragment() {
	
	private val viewModel by viewModels<SubscriptionListViewModel> { SubscriptionListViewModel }
	private lateinit var binding: FragmentSubscriptionsListBinding
	private val activity: MainActivity get() = requireActivity() as MainActivity
	
	private lateinit var adapter: GroupieAdapter
	private val header: HomeHeaderAdapterItem by lazy {
		HomeHeaderAdapterItem(
			viewModel.totalView,
			preferredCurrency = Currency.EUR,
			amount = 0.0,
			onClick = {
				viewModel.totalView = when (viewModel.totalView) {
					TotalView.Yearly -> TotalView.Weekly
					TotalView.Monthly -> TotalView.Yearly
					TotalView.Weekly -> TotalView.Monthly
				}
			}
		)
	}
	private val sectionBanner by lazy { Section() }
	private val sectionActive by lazy { Section() }
	private val expandableHeader by lazy { ExpandableHeaderItem() }
	private val sectionInactive by lazy {
		ExpandableSection(expandableHeader, false)
	}
	
	override fun onCreateView(
		inflater: LayoutInflater, container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		binding = FragmentSubscriptionsListBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		
		postponeEnterTransition()
		view.doOnPreDraw { startPostponedEnterTransition() }
		
		setFragmentResultListener("key") { _, bundle ->
			bundle.getString("Message")?.let {
				binding.root.snackbar(it)
			}
		}
		
		if (false) ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			
			val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
			binding.appBarLayout.updatePadding(top = bars.top)
			
			insets
		}
		binding.recyclerView.applyInsetter {
			type(/*statusBars = true, */navigationBars = true) {
				padding(/*top = true, */bottom = true)
			}
		}
		binding.recyclerView.itemAnimator = SmoothItemAnimator()
		
		initViews()
		
		collectFlow()
	}
	
	private fun initViews() {
		
		if (!::adapter.isInitialized) adapter = GroupieAdapter().also { adapter ->
			adapter.add(sectionBanner)
			adapter.add(sectionActive)
		}
		
		binding.recyclerView.adapter = adapter
		binding.recyclerView.addItemDecoration(
			AutoLayoutDecoration(
				"CurrencyItem",
				spaceBetween = 8.iDp,
				left = 8.iDp,
				right = 8.iDp
			)
		)
		
		binding.header.apply {
			textTotal.setCharacterLists("0123456789")
			textTotal.animationInterpolator = FastOutExtraSlowInInterpolator()
			val font = ResourcesCompat.getFont(binding.root.context, R.font.font_dm_ticker_large)
			textTotal.typeface = font
			root.setOnClickListener {
				viewModel.totalView = when (viewModel.totalView) {
					TotalView.Yearly -> TotalView.Weekly
					TotalView.Monthly -> TotalView.Yearly
					TotalView.Weekly -> TotalView.Monthly
				}
			}
		}
		
		binding.buttonTest.setOnClickListener {
			activity.setFabShifted(activity.isShifted.not())
		}
	}
	
	private fun collectFlow() {
		
		if (BuildConfig.DEBUG) (requireContext().applicationContext as FiniteApp).container.upgradeState
			.collectOn(viewLifecycleOwner) { state ->
				binding.header.headerTitle.text = if (state.isPro) "finite pro" else "finite"
			}
		
		viewModel.upgradeState.collectOn(viewLifecycleOwner) {
			if (it.isIllegalPro) {
				if (sectionBanner.itemCount == 0) sectionBanner.add(BannerRemindersSuspendedItem(
					onClick = {
						UpgradeSheet().show(requireActivity().supportFragmentManager, null)
					}
				))
			}
			else sectionBanner.clear()
		}
		
		viewModel.genderEquality.collectOn(viewLifecycleOwner) {
			updateUi(it)
		}
	}
	
	private suspend fun updateUi(model: SubscriptionListUiModel) {
		
		// TODO empty view
//		model.isEmpty
		
		binding.header.apply {
			textCurrencySign.text = model.defaultCurrency.let { it.symbol ?: it.iso4217Alpha }
			textTotal.text =  formatPrice(model.timeframeAmount)
			textView.text = when (model.timeframe) {
				TotalView.Yearly -> R.string.period_yearly
				TotalView.Monthly -> R.string.period_monthly
				TotalView.Weekly -> R.string.period_weekly
			}.let { getString(it) }
		}
		
		newItemMapper
			.partially2(model.colorOptions)
			.let {
				withContext(Dispatchers.Default) { model.active.map(it) to model.inactive.map(it) }
			}
			.let { (active, inactive) ->
				sectionActive.update(active)
				sectionInactive.update(inactive)
			}
		
		if (model.inactive.isEmpty()) {
			adapter.getAdapterPosition(sectionInactive).let { if (it != -1) adapter.remove(sectionInactive) }
		} else {
			adapter.getAdapterPosition(sectionInactive).let { if (it == -1) adapter.add(sectionInactive) }
		}
	}
	
	private val newItemMapper: (SubscriptionItemUiModel, ColorOptions) -> SubscriptionAdapterItem = {
		(model, currency, amount, showTimeLeft), options ->
		
		SubscriptionAdapterItem(
			model,
			currency,
			amount,
			showTimeLeft,
			palette = requireContext().makeItemColors(model.color, options),
			onClick = { cardView ->
				navigateToDetail(model, cardView)
			},
			onLongClick = {
				startActivity(Intent(requireActivity(), SubscriptionEditorActivity::class.java)
					.putExtra("Subscription", model))
			}
		)
	}
	
	private fun navigateToDetail(model: Subscription, cardView: MaterialCardView) {
		
		exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true)
		reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false)
		
		val extras = FragmentNavigatorExtras(cardView to "card_detail")
		
		findNavController().navigate(
			R.id.FragmentSubscriptionDetails,
			Bundle().apply {
				putInt("ID", model.id)
			},
			null,
			extras
		)
	}
}