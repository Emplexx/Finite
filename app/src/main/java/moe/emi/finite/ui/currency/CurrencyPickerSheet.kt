package moe.emi.finite.ui.currency

import android.content.Context
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import moe.emi.finite.databinding.LayoutSheetCurrencyBinding
import moe.emi.finite.dump.AlphaOnLiftListener
import moe.emi.finite.dump.deco.tableViewDecor
import moe.emi.finite.dump.setLiftOnScrollListener
import moe.emi.finite.service.data.Currency

class CurrencyPickerSheet(
	context: Context,
	val onSelect: (Currency) -> Unit,
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetCurrencyBinding
	
	val decorator by lazy { context.tableViewDecor() }
	
	private val adapter by lazy { GroupieAdapter() }
	private val section by lazy { Section() }
	
	val list = Currency.values().toList()
	var filteredList = list
	
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
		
		adapter.add(section)
		
		binding.recyclerView.setLiftOnScrollListener(AlphaOnLiftListener(binding.diviver))
		binding.recyclerView.addItemDecoration(decorator)
		
		binding.recyclerView.adapter = adapter
		updateRecycler()
		
		initViews()
	}
	
	private fun initViews() {
		binding.searchBar.field.doAfterTextChanged { editable ->
			if (editable.isNullOrBlank()) filteredList = list
			else filteredList = list.filter {
				it.iso4217Alpha.lowercase().contains(editable.toString().lowercase())
						|| it.fullName(context).lowercase().contains(editable.toString().lowercase())
			}
			updateRecycler()
		}
	}
	
	private fun updateRecycler() {
		section.update(filteredList.map {
			CurrencyAdapterItem(it) {
				selectAndDismiss(it)
			}
		})
	}
	
	private fun selectAndDismiss(currency: Currency) {
		onSelect(currency)
		this.dismiss()
	}
	
}