package moe.emi.finite.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.material.transition.MaterialSharedAxis
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.launch
import moe.emi.finite.MainActivity
import moe.emi.finite.MainViewModel
import moe.emi.finite.R
import moe.emi.finite.SecondFragment
import moe.emi.finite.SubscriptionEditorActivity
import moe.emi.finite.databinding.FragmentSubscriptionsListBinding
import moe.emi.finite.dump.HasSnackbarAnchor
import moe.emi.finite.dump.Response
import moe.emi.finite.dump.forEvery
import moe.emi.finite.dump.iDp
import moe.emi.finite.dump.snackbar
import moe.emi.finite.service.data.BillingPeriod
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.data.Rate
import moe.emi.finite.service.data.Subscription
import moe.emi.finite.service.repo.SubscriptionsRepo
import moe.emi.finite.ui.colors.makeItemColors

class SubscriptionsListFragment : Fragment() {
	
	private val mainViewModel by activityViewModels<MainViewModel>()
	private val viewModel by viewModels<SubscriptionListViewModel>()
	private lateinit var binding: FragmentSubscriptionsListBinding
	
	private val activity: MainActivity
		get() = requireActivity() as MainActivity
	
	private val defCurrency: Currency
		get() = viewModel.settings.preferredCurrency
	
	private lateinit var adapter: GroupieAdapter
	private val header: HomeHeaderAdapterItem by lazy {
		HomeHeaderAdapterItem(
			viewModel.totalView,
			preferredCurrency = defCurrency,
			amount = 0.0,
			onClick = {
				viewModel.totalView = when (viewModel.totalView) {
					TotalView.Yearly -> TotalView.Weekly
					TotalView.Monthly -> TotalView.Yearly
					TotalView.Weekly -> TotalView.Monthly
				}
				updateTotal()
			}
		)
	}
	private val sectionHeader by lazy { Section() }
	private val sectionActive by lazy { Section() }
	
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
		
		setFragmentResultListener("key") { key, bundle ->
			bundle.getString("Message")?.let {
				binding.root.snackbar(it)
			}
		}
		
		binding.recyclerView.applyInsetter {
			type(statusBars = true, navigationBars = true) {
				padding(top = true, bottom = true)
			}
		}
		
		initViews()
		
		viewModel.getSubscriptions()
		observe()
	}
	
	private fun initViews() {
		adapter = GroupieAdapter()
		
		adapter.add(sectionHeader)
		adapter.add(sectionActive)
		binding.recyclerView.adapter = adapter
		binding.recyclerView.addItemDecoration(
			AutoLayoutDecoration(
				"CurrencyItem",
				spaceBetween = 8.iDp,
				left = 8.iDp,
				right = 8.iDp
			)
		)
	}
	
	private fun observe() {
		viewModel.subscriptions.observe(viewLifecycleOwner) { subscriptions ->
			
			if (subscriptions.isEmpty()) {
				sectionHeader.clear()
				activity.setBottomBarVisibility(false)
			} else {
				activity.setBottomBarVisibility(true)
				sectionHeader.update(listOf(header))
			}
			
			sectionActive.update(subscriptions
				.map(itemMapper)
			)
			
			updateTotal()
		}
		
		viewModel.totalViewFlow.observe(viewLifecycleOwner) { totalView ->
			
			sectionActive.forEvery<SubscriptionAdapterItem> { group ->
				val subscription = group.model
				val rate = viewModel.rates.find { it.code == subscription.currency.iso4217Alpha } ?: Rate.EUR
				val preferredRate = viewModel.rates.find { it.code == defCurrency.iso4217Alpha } ?: Rate.EUR
				
				group.updateAmount(
					convert(subscription.price, rate, preferredRate, totalView, subscription.period)
				)
			}
		}
		
		viewModel.settingsFlow.observe(viewLifecycleOwner) { settings ->

			sectionActive.forEvery<SubscriptionAdapterItem> { group ->
				group.updateCurrency(settings.preferredCurrency)
				group.updatePalette(requireContext().makeItemColors(group.model.color))
			}
		}
		
		viewModel.showTimeLeftFlow.observe(viewLifecycleOwner) { bool ->
			sectionActive.forEvery<SubscriptionAdapterItem> {
				it.updateShowTimeLeft(bool)
			}
		}
		
		viewModel.ratesUpdateState.observe(viewLifecycleOwner) { response ->
			when (response) {
				Response.Loading -> binding.root.snackbar("Updating currency rates...")
				is Response.Failure -> binding.root.snackbar("Error updating currency rates")
				is Response.Success -> Unit
			}
		}
	}
	
	private val itemMapper: (Subscription) -> SubscriptionAdapterItem =
		{subscription ->
			
			val rate = viewModel.rates.find { it.code == subscription.currency.iso4217Alpha } ?: Rate.EUR
			val preferredRate = viewModel.rates.find { it.code == defCurrency.iso4217Alpha } ?: Rate.EUR
			
			SubscriptionAdapterItem(
				subscription,
				defCurrency,
				convert(subscription.price, rate, preferredRate, viewModel.totalView, subscription.period),
				viewModel.showTimeLeftFlow .value ?: false,
				palette = requireContext().makeItemColors(subscription.color),
				onClick = { cardView ->
					navigateToDetail(subscription, cardView)
				},
				onLongClick = {
					startActivity(Intent(requireActivity(), SubscriptionEditorActivity::class.java)
						.putExtra("Subscription", subscription))
				}
			)
			
		}
	
	private fun updateTotal() {
		
		val rates = viewModel.rates
		
		header.updateTotal(
			viewModel.totalView,
			viewModel.subscriptions.value.orEmpty().sumOf { subscription ->
				
				val rate = rates.find { it.code == subscription.currency.iso4217Alpha } ?: Rate.EUR
				val preferredRate = rates.find { it.code == defCurrency.iso4217Alpha } ?: Rate.EUR
				
				convert(subscription.price, rate, preferredRate,
					viewModel.totalView, subscription.period)
					.amountMatchedToTimeframe
				
//				when (viewModel.totalView) {
//					TotalView.Yearly -> it.priceEveryYear
//					TotalView.Monthly -> it.priceEveryMonth
//					TotalView.Weekly -> it.priceEveryWeek
//				}
			},
			defCurrency
		)
	}
	
	fun navigateToDetail(model: Subscription, cardView: MaterialCardView) {
		
		exitTransition = MaterialSharedAxis(MaterialSharedAxis.Y, true).apply {
		
		}
		reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Y, false).apply {
		
		}
		
		val extras = FragmentNavigatorExtras(cardView to "card_detail")
		
		findNavController().navigate(
			R.id.FragmentSubscriptionDetails,
			Bundle().apply {
				putInt("ID", model.id)
			},
			null,
			extras
		)

//		findNavController().navigate(
//			SubscriptionsListFragmentDirections.actionFirstFragmentToSecondFragment(),
//			extras
//		)
	}
	
	
	data class ConvertedAmount(
		val timeframe: TotalView,
		val amountMatchedToTimeframe: Double,
		val amountOriginal: Double,
		val from: Rate,
		val to: Rate
	)
	
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