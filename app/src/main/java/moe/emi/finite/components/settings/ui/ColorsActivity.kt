package moe.emi.finite.components.settings.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.slider.Slider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.R
import moe.emi.finite.components.settings.store.AppSettings
import moe.emi.finite.components.settings.store.SettingsStore
import moe.emi.finite.components.settings.store.editor
import moe.emi.finite.databinding.ActivitySettingsBinding
import moe.emi.finite.di.memberInjection
import moe.emi.finite.dump.isDarkTheme
import kotlin.math.roundToInt

class ColorsActivity : AppCompatActivity() {
	
	private lateinit var binding: ActivitySettingsBinding
	private lateinit var settingsStore: SettingsStore
	
	init {
		memberInjection {
			settingsStore = it.settingsStore
		}
	}
	
	private val editSettings = settingsStore.editor(this)
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivitySettingsBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		lifecycleScope.launch {
			
			repeatOnLifecycle(Lifecycle.State.CREATED) {
				
				initLayout()
				initListeners()
				
				loadOnce(settingsStore.data.first())
				
				settingsStore.data.collect {
					loadData(it)
				}
			}
		}
	}
	
	private fun initLayout() {
		binding.rowHarmonize.switchView.setText(R.string.setting_harmonize)
		binding.footerHarmonize.text.setText(R.string.setting_harmonize_description)
		
		binding.rowNormalize.switchView.setText(R.string.setting_normalise)
		binding.footerNormalize.text.setText(R.string.setting_normalise_description)
		
		binding.sliderBrightness.value = 10f - binding.sliderBrightness.value
	}
	
	private fun initListeners() {
		
		binding.rowHarmonize.switchView.setOnCheckedChangeListener { _, isChecked ->
			editSettings { it.copy(harmonizeColors = isChecked) }
		}
		
		binding.rowNormalize.switchView.setOnCheckedChangeListener { _, isChecked ->
			editSettings { it.copy(normalizeColors = isChecked) }
		}
		
		binding.sliderBrightness.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
			override fun onStartTrackingTouch(slider: Slider) {}
			
			override fun onStopTrackingTouch(slider: Slider) {
				val v =
					if (isDarkTheme) slider.value
					else 10f - slider.value
				
				editSettings { it.copy(normalizeFactor = v.roundToInt()) }
			}
			
		})
	}
	
	private fun loadOnce(it: AppSettings) {
		binding.sliderBrightness.value =
			if (isDarkTheme) it.normalizeFactor.toFloat()
			else 10f - it.normalizeFactor
	}
	
	private fun loadData(it: AppSettings) {
		
		binding.rowHarmonize.switchView.isChecked = it.harmonizeColors
		binding.rowNormalize.switchView.isChecked = it.normalizeColors
	}
}