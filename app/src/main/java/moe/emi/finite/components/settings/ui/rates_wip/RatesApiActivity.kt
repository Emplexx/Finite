package moe.emi.finite.components.settings.ui.rates_wip

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import moe.emi.finite.core.rates.api.ApiProvider
import moe.emi.finite.databinding.ActivityRatesApiBinding
import moe.emi.finite.dump.collectOn

class RatesApiActivity : AppCompatActivity() {
	
	private val viewModel by viewModels<RatesApiViewModel>()
	private lateinit var binding: ActivityRatesApiBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		
		binding = ActivityRatesApiBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.headerProvider.text.text = "Provider"
		binding.rowInforEuro.apply {
			textLabel.text = "InforEuro"
			textDescription.text = "151 currencies\nRates update at the end of each month"
			root.setOnClickListener {
				viewModel.provider = ApiProvider.InforEuro
			}
			radioButton.setOnClickListener {
				viewModel.provider = ApiProvider.InforEuro
			}
		}
		binding.rowOpenExchangeRates.apply {
			textLabel.text = "Open Exchange Rates"
			textDescription.text = "169 currencies\nRates update hourly\nYou will need to provide an App ID"
			root.setOnClickListener {
				viewModel.provider = ApiProvider.ExchangeRatesApi
				
			}
			radioButton.setOnClickListener {
				viewModel.provider = ApiProvider.ExchangeRatesApi
			}
		}
		
		
		binding.headerStatus.text.text = "Status"
		
		
		viewModel.providerFlow.collectOn(this) {
			binding.rowInforEuro.radioButton.isChecked = it == ApiProvider.InforEuro
			binding.rowOpenExchangeRates.radioButton.isChecked = it == ApiProvider.ExchangeRatesApi
		}
	}
}