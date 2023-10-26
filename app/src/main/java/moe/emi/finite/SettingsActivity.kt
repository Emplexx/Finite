package moe.emi.finite

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import moe.emi.finite.databinding.ActivitySettingsBinding
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.dump.getStorable
import moe.emi.finite.dump.isDarkTheme
import moe.emi.finite.dump.setStorable
import moe.emi.finite.service.datastore.storeGeneral
import moe.emi.finite.ui.currency.CurrencyPickerSheet
import kotlin.math.roundToInt
import kotlin.properties.Delegates.observable

class SettingsActivity : AppCompatActivity() {
	
	private lateinit var binding: ActivitySettingsBinding
	
	private var settings: AppSettings by observable(AppSettings()) { _, old, new ->
		if (old != new) lifecycleScope.launch {
			storeGeneral.setStorable(new)
		}
	}
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivitySettingsBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		lifecycleScope.launch {
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				
				initLayout()
				initListeners()
				
				storeGeneral.getStorable<AppSettings>().collect {
					settings = it
					loadData(it)
				}
			}
		}
	}
	
	fun initLayout() {
		binding.rowHarmonize.switchView.text = "Harmonise colours"
		binding.footerHarmonize.text.text = "Slightly adjust hue of subscription colours so they better match the theme of your device and You"
		
		binding.rowNormalize.switchView.text = "Normalise colours"
		binding.footerNormalize.text.text = "Make subscription colours the same tone so they are more consistent and match light or dark theme"
		
		binding.sliderBrightness.value = 10f - binding.sliderBrightness.value
	}
	
	fun initListeners() {
		Build.VERSION_CODES.S_V2
		binding.rowHarmonize.switchView.setOnCheckedChangeListener { _, isChecked ->
			settings = settings.copy(harmonizeColors = isChecked)
		}
		
		binding.rowNormalize.switchView.setOnCheckedChangeListener { _, isChecked ->
			settings = settings.copy(normalizeColors = isChecked)
		}
		
		binding.sliderBrightness.addOnChangeListener { slider, value, fromUser ->
			val v =
				if (isDarkTheme) value
				else 10f - value
			if (fromUser) settings = settings.copy(normalizeFactor = v.roundToInt())
		}
		
		binding.rowCurrency.setOnClickListener {
			CurrencyPickerSheet(this) {
				settings = settings.copy(preferredCurrency = it)
			}
				.show()
		}
	}
	
	fun loadData(it: AppSettings) {
		
		binding.rowHarmonize.switchView.isChecked = it.harmonizeColors
		
		binding.rowNormalize.switchView.isChecked = it.normalizeColors
		binding.sliderBrightness.value =
			if (isDarkTheme) it.normalizeFactor.toFloat()
			else 10f - it.normalizeFactor
		
		binding.textCurrency.text = it.preferredCurrency.fullName
	}
}