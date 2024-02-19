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
import moe.emi.finite.BuildConfig
import moe.emi.finite.R
import moe.emi.finite.databinding.LayoutSheetSettingsBinding
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.AppTheme
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.editSettings
import moe.emi.finite.service.datastore.set
import moe.emi.finite.ui.currency.CurrencyPickerSheet
import moe.emi.finite.ui.settings.backup.BackupActivity

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
	
	private fun initLayout() {
		
		binding.version.text = BuildConfig.VERSION_NAME
		
		binding.rowCurrency.textLabel.setText(R.string.setting_default_currency)
		binding.rowTheme.textLabel.setText(R.string.setting_theme)
		
		binding.rowColors.apply {
			layoutIcon.visible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_palette_fill_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.pink))
			textLabel.setText(R.string.setting_colors_title)
			textValue.visible = false
		}
		binding.rowBackup.apply {
			layoutIcon.visible = true
			icon.setImageDrawable(context.drawable(R.drawable.ic_backup_restore_24))
			backgroundColor.setCardBackgroundColor(context.getColor(R.color.blue))
			textLabel.setText(R.string.setting_backup_title)
			textValue.visible = false
		}
	}
	
	private fun initListeners() {
		binding.rowApp.setOnClickListener {
			context.startActivity(Intent(context, AboutActivity::class.java))
		}
		
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
					it.add(0, 0, 0, R.string.setting_theme_follow_system)
					it.add(0, 1, 0, R.string.setting_theme_light)
					it.add(0, 2, 0, R.string.setting_theme_dark)
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
			context.startActivity(Intent(context, ColorsActivity::class.java))
		}
		binding.rowBackup.root.setOnClickListener {
			context.startActivity(Intent(context, BackupActivity::class.java))
		}
	}
	
	private fun loadData(it: AppSettings) {
		binding.rowCurrency.textValue.text = it.preferredCurrency.iso4217Alpha
		binding.rowTheme.textValue.text = when (it.appTheme) {
			AppTheme.Unspecified -> R.string.setting_theme_follow_system
			AppTheme.Light -> R.string.setting_theme_light
			AppTheme.Dark -> R.string.setting_theme_dark
		}.let { context.getString(it) }
		
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