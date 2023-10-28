package moe.emi.finite.ui.settings

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.convenience.drawable
import moe.emi.finite.R
import moe.emi.finite.SettingsActivity
import moe.emi.finite.databinding.LayoutSheetSettingsBinding
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.AppTheme
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.editSettings
import moe.emi.finite.service.datastore.set
import moe.emi.finite.ui.currency.CurrencyPickerSheet

class SettingsSheet(
	context: Context
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetSettingsBinding
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetSettingsBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		
		behavior.state = BottomSheetBehavior.STATE_EXPANDED
		behavior.skipCollapsed = true
		
		lifecycleScope.launch {
			initLayout()
			initListeners()
			context.appSettings.collect {
				loadData(it)
			}
		}
	}
	
	private suspend fun initLayout() {
		binding.rowCurrency.textLabel.text = "Default currency"
		binding.rowTheme.textLabel.text = "Theme"
		
		binding.rowColors.apply {
			layoutIcon.visible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_palette_fill_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.pink))
			textLabel.text = "Colours"
			textValue.visible = false
		}
	}
	
	private fun initListeners() {
		binding.rowCurrency.root.setOnClickListener {
			CurrencyPickerSheet(context)
				{ currency ->
					editSettings { it.copy(preferredCurrency = currency) }
				}
				.show()
		}
		binding.rowTheme.root.setOnClickListener {
			PopupMenu(context, binding.rowTheme.root, Gravity.END).apply {
				menu.also {
					it.add(0, 0, 0, "Follow system")
					it.add(0, 1, 0, "Light")
					it.add(0, 2, 0, "Dark")
				}
				setOnMenuItemClickListener {
					when (it.itemId) {
						0 -> {
							setTheme(AppTheme.Unspecified)
							true
						}
						1 -> {
							setTheme(AppTheme.Light)
							true
						}
						2 -> {
							setTheme(AppTheme.Dark)
							true
						}
						else -> false
					}
				}
			}.show()
		}
		
		binding.rowColors.root.setOnClickListener {
			context.startActivity(Intent(context, SettingsActivity::class.java))
		}
	}
	
	private suspend fun loadData(it: AppSettings) {
		binding.rowCurrency.textValue.text = it.preferredCurrency.iso4217Alpha
		binding.rowTheme.textValue.text = when (it.appTheme) {
			AppTheme.Unspecified -> "Follow system"
			AppTheme.Light -> "Light"
			AppTheme.Dark -> "Dark"
		}
		
	}
	
	private fun setTheme(theme: AppTheme) {
		lifecycleScope.launch {
			context.appSettings.first().copy(appTheme = theme).set()
		}
		when (theme) {
			AppTheme.Unspecified ->
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
			AppTheme.Light ->
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
			AppTheme.Dark ->
				AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
		}
	}
	
}