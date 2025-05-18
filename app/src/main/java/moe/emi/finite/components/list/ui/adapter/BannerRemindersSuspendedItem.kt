package moe.emi.finite.components.list.ui.adapter

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.BannerRemindersSuspendedBinding

class BannerRemindersSuspendedItem(
	val onClick: () -> Unit
) : BindableItem<BannerRemindersSuspendedBinding>() {
	
	override fun bind(binding: BannerRemindersSuspendedBinding, position: Int) {
		binding.buttonUpgrade.setOnClickListener {
			onClick()
		}
	}
	
	override fun getLayout() = R.layout.banner_reminders_suspended
	override fun initializeViewBinding(view: View) = BannerRemindersSuspendedBinding.bind(view)
	
}