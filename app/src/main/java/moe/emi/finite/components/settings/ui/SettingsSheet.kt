package moe.emi.finite.components.settings.ui

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import convenience.resources.drawable
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import moe.emi.finite.BuildConfig
import moe.emi.finite.FiniteApp
import moe.emi.finite.R
import moe.emi.finite.components.currency.CurrencyPickerSheet
import moe.emi.finite.components.settings.store.AppSettings
import moe.emi.finite.components.settings.store.AppTheme
import moe.emi.finite.components.settings.store.appSettings
import moe.emi.finite.components.settings.store.editSettings
import moe.emi.finite.components.settings.ui.backup.BackupActivity
import moe.emi.finite.components.settings.ui.rates_wip.RatesApiActivity
import moe.emi.finite.components.upgrade.UpgradeSheet
import moe.emi.finite.components.upgrade.cache.UpgradeState
import moe.emi.finite.databinding.LayoutSheetSettingsBinding

class SettingsSheet(
	context: Context
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetSettingsBinding
	private lateinit var upgradeState: Flow<UpgradeState>
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetSettingsBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		upgradeState = (context.applicationContext as FiniteApp).container.upgradeState
		
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
			launch {
				upgradeState.collect {
					binding.sectionUpgrade.isVisible = !it.isPro
				}
			}
			context.appSettings.collect {
				updateData(it)
			}
		}
	}
	
	private fun initLayout() {
		
		binding.version.text = BuildConfig.VERSION_NAME
		
		binding.rowUpgrade.apply {
			icon.setImageResource(R.drawable.ic_upgrade_24)
			textLabel.text = "Upgrade"
			root.setOnClickListener {
				dismiss()
				(ownerActivity as? FragmentActivity)?.let {
					UpgradeSheet().show(it.supportFragmentManager, null)
				}
			}
		}
		
		binding.rowCurrency.textLabel.setText(R.string.setting_default_currency)
		binding.rowTheme.textLabel.setText(R.string.setting_theme)
		
		binding.rowColors.apply {
			layoutIcon.isVisible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_palette_fill_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.pink))
			textLabel.setText(R.string.setting_colors_title)
			textValue.isVisible = false
		}
		binding.rowRates.apply {
			layoutIcon.isVisible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_refresh_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.mint))
			textLabel.setText("Rates")
			textValue.isVisible = false
		}
		binding.rowBackup.apply {
			layoutIcon.isVisible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_backup_restore_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.blue))
			textLabel.setText(R.string.setting_backup_title)
			textValue.isVisible = false
		}
		
		binding.cardDev.isVisible = BuildConfig.DEBUG
		binding.rowDev.apply {
			textLabel.text = "Dev options"
			textValue.isVisible = false
		}
	}
	
	private fun initListeners() {
		binding.rowApp.setOnClickListener {
			context.startActivity(Intent(context, AboutActivity::class.java))
		}
		
		binding.rowCurrency.root.setOnClickListener {
			CurrencyPickerSheet(context, false)
				{ currency ->
					editSettings { it.copy(preferredCurrency = currency) }
				}
				.show()
		}
		binding.rowTheme.root.setOnClickListener {
			PopupMenu(context, binding.rowTheme.root, Gravity.END).apply {
				menu.also {
					it.add(0, 0, 0, R.string.setting_theme_follow_system)
					it.add(0, 1, 0, R.string.setting_theme_light)
					it.add(0, 2, 0, R.string.setting_theme_dark)
				}
				setOnMenuItemClickListener {
					when (it.itemId) {
						0 -> setAndUpdateTheme(AppTheme.Unspecified)
						1 -> setAndUpdateTheme(AppTheme.Light)
						2 -> setAndUpdateTheme(AppTheme.Dark)
					}
					when (it.itemId) {
						0, 1, 2 -> true
						else -> false
					}
				}
			}.show()
		}
		
		binding.rowColors.root.setOnClickListener {
			context.startActivity(Intent(context, ColorsActivity::class.java))
		}
		binding.rowRates.root.setOnClickListener {
			context.startActivity(Intent(context, RatesApiActivity::class.java))
		}
		binding.rowBackup.root.setOnClickListener {
			context.startActivity(Intent(context, BackupActivity::class.java))
		}
		
		binding.rowDev.root.setOnClickListener {
			MaterialAlertDialogBuilder(context)
				.setItems(arrayOf("Clear rates")) { _, id ->
					when (id) {
						0 -> lifecycleScope.launch {
							(context.applicationContext as FiniteApp).container.ratesRepo.clearRates()
						}
					}
				}
				.show()
		}
	}
	
	private fun updateData(it: AppSettings) {
		binding.rowCurrency.textValue.text = it.preferredCurrency.iso4217Alpha
		binding.rowTheme.textValue.text = when (it.appTheme) {
			AppTheme.Unspecified -> R.string.setting_theme_follow_system
			AppTheme.Light -> R.string.setting_theme_light
			AppTheme.Dark -> R.string.setting_theme_dark
		}.let { context.getString(it) }
		
	}
	
	private fun setAndUpdateTheme(theme: AppTheme) {
		
		editSettings { it.copy(appTheme = theme) }
		
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