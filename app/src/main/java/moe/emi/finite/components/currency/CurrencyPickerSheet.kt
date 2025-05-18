package moe.emi.finite.components.currency

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.xwray.groupie.GroupieAdapter
import com.xwray.groupie.Section
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.emi.finite.R
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.preferences.Pref
import moe.emi.finite.core.preferences.get
import moe.emi.finite.core.preferences.set
import moe.emi.finite.core.ui.animator.SmoothItemAnimator
import moe.emi.finite.core.ui.decoration.RoundDecoration
import moe.emi.finite.core.ui.decoration.divider
import moe.emi.finite.core.ui.decoration.showBetween
import moe.emi.finite.databinding.LayoutSheetCurrencyBinding
import moe.emi.finite.di.memberInjection
import moe.emi.finite.dump.android.setLiftOnScrollListener
import moe.emi.finite.dump.collectOn
import moe.emi.finite.dump.fDp
import moe.emi.finite.dump.iDp
import moe.emi.finite.dump.requestDisplayDimensions

class CurrencyPickerSheet(
	context: Context,
	private val saveHistory: Boolean,
	private val onSelect: (Currency) -> Unit,
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetCurrencyBinding
	
	private lateinit var availableRates: Flow<List<Currency>>
	private lateinit var preferences: DataStore<Preferences>
	private lateinit var defaultCurrency: Flow<Currency>
	private lateinit var appScope: CoroutineScope
	
	private val currencyHistory by lazy { preferences[Pref.currencyHistory] }
	
	init {
		context.memberInjection {
			availableRates = ratesRepo.fetchedRates.map { it?.rates?.map { it.currency }?.sorted() ?: emptyList() }
			preferences = dataStore
			defaultCurrency = settingsStore.data.map { it.preferredCurrency }
			appScope = scope
		}
	}
	
	private val adapter by lazy { GroupieAdapter() }
	private val mainSection by lazy {
		Section().apply {
			setHeader(SpacerAdapterItem())
			setFooter(HintAdapterItem("Press enter to select first result"))
			setHideWhenEmpty(true)
		}
	}
	
	private val searchQuery = MutableStateFlow("")
	
	private val searchResults = combine(
		availableRates,
		currencyHistory,
		defaultCurrency,
		searchQuery
	) { rates, history, default, query ->
		
		val top = if (default in history) emptyList() else listOf(default)
		val other = rates.filterNot { it in history || it == default }
		
		val filterByQuery: (String) -> (Currency) -> Boolean = { query1 ->
			{
				it.iso4217Alpha.lowercase().contains(query1.lowercase())
						|| it.fullName(context).lowercase().contains(query1.lowercase())
			}
		}
		
		if (query.isBlank()) {
			Triple(top, history, other)
		}
		else Triple(
			top.filter(filterByQuery(query)),
			history.filter(filterByQuery(query)),
			other.filter(filterByQuery(query)),
		)
	}
	
	private val imeEvents = callbackFlow {
		binding.searchBar.field.setOnEditorActionListener { v, actionId, event ->
			when (actionId) {
				EditorInfo.IME_ACTION_GO, EditorInfo.IME_ACTION_DONE -> {
					trySend(actionId)
					true
				}
				else -> false
			}
		}
		awaitClose {
			binding.searchBar.field.setOnEditorActionListener(null)
		}
	}
	
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
		
		adapter.add(mainSection)
		
		binding.recyclerView.setLiftOnScrollListener(AlphaOnLiftListener(binding.diviver))
		binding.recyclerView.itemAnimator = SmoothItemAnimator(1f, 1f)
		binding.recyclerView.addItemDecoration(RoundDecoration(8.fDp))
		binding.recyclerView.divider {
			paddingStart = 16.iDp
			paddingEnd = 16.iDp
			makeSpace = true
			showUnder(binding.recyclerView.showBetween(R.layout.item_currency))
		}
		binding.recyclerView.adapter = adapter
		
		binding.searchBar.field.doAfterTextChanged { editable ->
			searchQuery.update { editable?.toString() ?: "" }
		}
		
		searchResults.collectOn(this) { (top, history, other) ->
			
			val default = defaultCurrency.first()
			val mapper = { it: Currency ->
				CurrencyAdapterItem(it) {
					selectAndDismiss(it, default)
				}
			}
			
			listOf(
				top.map(mapper),
				history.map(mapper),
				other.map(mapper)
			)
				.filter { it.isNotEmpty() }
				.flatMapIndexed { index, items ->
					if (index == 0) items
					else listOf(SpacerAdapterItem()) + items
				}
				.let(mainSection::update)
			
		}
		
		imeEvents.collectOn(this) {
			
			val (a, b, c) = searchResults.first()
			val result = a.firstOrNull() ?: b.firstOrNull() ?: c.firstOrNull()
			
			if (result != null) selectAndDismiss(result, defaultCurrency.first())
		}
		
	}
	
	private fun selectAndDismiss(currency: Currency, default: Currency) {
		onSelect(currency)
		if (saveHistory && currency != default) appScope.launch {
			val newHistory = listOf(currency).plus(currencyHistory.first()).toSet().toList()
			preferences.set(Pref.currencyHistory, newHistory)
		}
		this.dismiss()
	}
	
}