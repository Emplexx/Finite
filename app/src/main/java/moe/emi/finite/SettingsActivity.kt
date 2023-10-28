package moe.emi.finite

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.databinding.ActivitySettingsBinding
import moe.emi.finite.dump.isDarkTheme
import moe.emi.finite.dump.setStorable
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.storeGeneral
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
				
				loadOnce(appSettings.first())
				
				appSettings.collect {
					settings = it
					loadData(it)
				}
			}
		}
	}
	
	fun initLayout() {
		binding.rowHarmonize.switchView.setText(R.string.setting_harmonize)
		binding.footerHarmonize.text.setText(R.string.setting_harmonize_description)
		
		binding.rowNormalize.switchView.setText(R.string.setting_normalise)
		binding.footerNormalize.text.setText(R.string.setting_normalise_description)
		
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
		
//		binding.sliderBrightness.addOnChangeListener { slider, value, fromUser ->
//			val v =
//				if (isDarkTheme) value
//				else 10f - value
//			if (fromUser) settings = settings.copy(normalizeFactor = v.roundToInt())
//		}
		binding.sliderBrightness.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
			override fun onStartTrackingTouch(slider: Slider) {}
			
			override fun onStopTrackingTouch(slider: Slider) {
				val v =
					if (isDarkTheme) slider.value
					else 10f - slider.value
				settings = settings.copy(normalizeFactor = v.roundToInt())
			}
			
		})
	}
	
	fun loadOnce(it: AppSettings) {
		Log.d("TAG", "factor: ${it.normalizeFactor}")
		binding.sliderBrightness.value =
			if (isDarkTheme) it.normalizeFactor.toFloat()
			else 10f - it.normalizeFactor
	}
	
	fun loadData(it: AppSettings) {
		
		binding.rowHarmonize.switchView.isChecked = it.harmonizeColors
		
		binding.rowNormalize.switchView.isChecked = it.normalizeColors
	}
}