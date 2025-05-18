package moe.emi.finite.components.currency

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.PreferenceFooterBinding

class HintAdapterItem(
	val text: String
) : BindableItem<PreferenceFooterBinding>() {
	
	override fun bind(binding: PreferenceFooterBinding, position: Int) {
		binding.text.text = text
	}
	
	override fun getLayout() = R.layout.preference_footer
	override fun initializeViewBinding(view: View) = PreferenceFooterBinding.bind(view)
	
}