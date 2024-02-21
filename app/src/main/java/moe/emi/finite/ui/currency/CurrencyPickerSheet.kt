package moe.emi.finite.ui.currency

import android.content.Context
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import moe.emi.finite.databinding.LayoutSheetCurrencyBinding
import moe.emi.finite.dump.AlphaOnLiftListener
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.deco.tableViewDecor
import moe.emi.finite.dump.requestDisplayDimensions
import moe.emi.finite.dump.setLiftOnScrollListener
import moe.emi.finite.service.data.Currency
import moe.emi.finite.service.repo.RatesRepo

class CurrencyPickerSheet(
	context: Context,
	val onSelect: (Currency) -> Unit,
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetCurrencyBinding
	
	private val decorator by lazy { context.tableViewDecor() }
	
	private val adapter by lazy { GroupieAdapter() }
	private val section by lazy { Section() }
	
//  private val list = Currency.values().toList()
	private val availableRates = RatesRepo.fetchedRates.map { it?.rates ?: emptyList() }
//	private val list = RatesRepo.getLocalRates()
	
	private val searchQuery = MutableStateFlow("")
	
	private val listState = availableRates
		.combine(searchQuery) { rates, query ->
			val available = rates.map { it.currency }.sorted()
			
			if (query.isBlank()) available
			else available.filter {
				it.iso4217Alpha.lowercase().contains(query.lowercase())
						|| it.fullName(context).lowercase().contains(query.lowercase())
			}
		}
	
//	private var filteredList = list
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetCurrencyBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.recyclerView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		binding.recyclerView.post {
			binding.recyclerView.minimumHeight = binding.recyclerView.measuredHeight
		}
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
		behavior.skipCollapsed = true
		window?.let {
			val (_, height) = requestDisplayDimensions(it)
			behavior.peekHeight = height
			binding.root.minimumHeight = height
		}
		
		adapter.add(section)
		
		binding.recyclerView.setLiftOnScrollListener(AlphaOnLiftListener(binding.diviver))
		binding.recyclerView.addItemDecoration(decorator)
		
		binding.recyclerView.adapter = adapter
//		updateRecycler()
		
		binding.searchBar.field.doAfterTextChanged { editable ->
//			filteredList =
//				if (editable.isNullOrBlank()) list
//				else list.filter {
//					it.iso4217Alpha.lowercase().contains(editable.toString().lowercase())
//							|| it.fullName(context).lowercase().contains(editable.toString().lowercase())
//				}
//			updateRecycler()
			searchQuery.update { editable?.toString() ?: "" }
		}
		
		listState.collectOn(this) { list ->
			list
				.map {
					CurrencyAdapterItem(it) {
						selectAndDismiss(it)
					}
				}
				.let(section::update)
		}
		
	}
	
//	private fun updateRecycler() {
//		section.update(filteredList.map {
//			CurrencyAdapterItem(it) {
//				selectAndDismiss(it)
//			}
//		})
//	}
	
	private fun selectAndDismiss(currency: Currency) {
		onSelect(currency)
		this.dismiss()
	}
	
}