package moe.emi.finite.ui.home

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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialSharedAxis
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import moe.emi.finite.MainActivity
import moe.emi.finite.R
import moe.emi.finite.databinding.FragmentSubscriptionsListBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.iDp
import moe.emi.finite.dump.snackbar
import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.ui.colors.makeItemColors
import moe.emi.finite.ui.editor.SubscriptionEditorActivity
import moe.emi.finite.ui.home.adapter.AutoLayoutDecoration
import moe.emi.finite.ui.home.adapter.HomeHeaderAdapterItem
import moe.emi.finite.ui.home.adapter.SubscriptionAdapterItem
import moe.emi.finite.ui.home.adapter.java.ExpandableHeaderItem
import moe.emi.finite.ui.home.adapter.java.ExpandableSection
import moe.emi.finite.ui.home.model.ConvertedAmount
import moe.emi.finite.ui.home.model.SubscriptionUiModel
import moe.emi.finite.ui.home.model.TotalView
import java.math.RoundingMode
import java.text.DecimalFormat

class SubscriptionListFragment : Fragment() {
	
	private val viewModel by viewModels<SubscriptionListViewModel>()
	
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
	private val sectionHeader by lazy { Section() }
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
		
//		binding.center.applyInsetter {
//			type(statusBars = true) {
//				margin(top = true)
//			}
//		}
		if (false) ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
			
			val bars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
			binding.appBarLayout.updatePadding(top = bars.top)
//			binding.rectangles.updateLayoutParams<MarginLayoutParams> {
//				topMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
//			}
			
			insets
		}
//		binding.rectangles.applyInsetter {
//			type(statusBars = true) {
//				margin(top = true)
//			}
//		}
		binding.recyclerView.applyInsetter {
			type(/*statusBars = true, */navigationBars = true) {
				padding(/*top = true, */bottom = true)
			}
		}
		
		initViews()
		
		viewModel.getSubscriptions()
		collectFlow()
	}
	
	private fun initViews() {
		
		if (::adapter.isInitialized.not()) adapter = GroupieAdapter().also { adapter ->
//			adapter.add(sectionHeader)
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
		
		viewModel.totalSpentFlow.collectOn(viewLifecycleOwner) {
				(view, amount, currency) ->
			header.updateTotal(view, amount, currency)
			
			binding.header.apply {
				
				textCurrencySign.text = currency.symbol ?: currency.iso4217Alpha
				textView.text = when (view) {
					TotalView.Yearly -> R.string.period_yearly
					TotalView.Monthly -> R.string.period_monthly
					TotalView.Weekly -> R.string.period_weekly
				}.let { getString(it) }
				
				textTotal.text =  DecimalFormat("0.00")
					.apply { roundingMode = RoundingMode.CEILING }
					.format(amount)
			}
			
			
		}
		
		viewModel.subscriptionsFlow.collectOn(viewLifecycleOwner) { subscriptions ->
			if (subscriptions.isEmpty()) {
				sectionHeader.clear()
				activity.setBottomBarVisibility(false)
			} else {
				activity.setBottomBarVisibility(true)
				sectionHeader.update(listOf(header))
			}
			
			val (active, inactive) = subscriptions.partition { it.model.active }
			
			sectionActive.update(active.map(newItemMapper), false)
			sectionInactive.update(inactive.map(newItemMapper), false)
			
			
			
			if (inactive.isEmpty()) {
				adapter.getAdapterPosition(sectionInactive).let { if (it != -1) adapter.remove(sectionInactive) }
			} else {
				adapter.getAdapterPosition(sectionInactive).let { if (it == -1) adapter.add(sectionInactive) }
			}
		}
		
	}
	
	private val newItemMapper: (SubscriptionUiModel) -> SubscriptionAdapterItem =
		{ (model, currency, amount, showTimeLeft) ->
			SubscriptionAdapterItem(
				model,
				currency,
				amount,
				showTimeLeft,
				palette = requireContext().makeItemColors(model.color),
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
	
	
	companion object {
		
		
		fun convert(
			amount: Double,
			from: Rate, to: Rate,
			timeframe: TotalView, period: BillingPeriod,
		): ConvertedAmount {
			val converted = amount / from.rate * to.rate
			return ConvertedAmount(
				timeframe,
				when (timeframe) {
					TotalView.Yearly -> period.priceEveryYear(converted)
					TotalView.Monthly -> period.priceEveryMonth(converted)
					TotalView.Weekly -> period.priceEveryWeek(converted)
				},
				converted,
				from, to
			)
		}
		
	}
}