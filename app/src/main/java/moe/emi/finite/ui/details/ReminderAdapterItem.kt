package moe.emi.finite.ui.details

import android.view.View
import com.xwray.groupie.Item
import com.xwray.groupie.viewbinding.BindableItem
import moe.emi.finite.R
import moe.emi.finite.databinding.ItemReminderBinding
import moe.emi.finite.service.data.Reminder
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ReminderAdapterItem(
	val reminder: Reminder,
	val onEdit: () -> Unit,
	val onRemove: () -> Unit,
) : BindableItem<ItemReminderBinding>() {
	
	override fun bind(binding: ItemReminderBinding, position: Int) {
		with(binding.root.context) {
			binding.textName.text = when (reminder.period) {
				null -> "Same day"
				else -> "${reminder.period.count} ${reminder.period.timespan.name} before"
			}
			
			val c = LocalTime.of(reminder.hours, reminder.minutes)
			binding.textCode.text = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(c)
			
			binding.buttonRemove.setOnClickListener { onRemove() }
			binding.root.setOnClickListener { onEdit() }
		}
	}
	
	override fun isSameAs(other: Item<*>): Boolean =
		when (other) {
			is ReminderAdapterItem -> reminder.id == other.reminder.id
			else -> super.isSameAs(other)
		}
	
	override fun hasSameContentAs(other: Item<*>): Boolean =
		when (other) {
			is ReminderAdapterItem -> reminder == other.reminder
			else -> super.isSameAs(other)
		}
	
	override fun getLayout() = R.layout.item_reminder
	override fun initializeViewBinding(view: View) = ItemReminderBinding.bind(view)
	
}