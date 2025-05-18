package moe.emi.finite.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import moe.emi.finite.core.model.Currency
import moe.emi.finite.core.model.Subscription
import moe.emi.finite.core.model.Timespan
import java.io.Serializable

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
	
	val name: String = "",
	val description: String = "",
	val color: Int? = null,
	
	val amount: Double = 0.0,
	val currency: Currency,
	
	val startedOn: Long? = null,
	val periodAmount: Int = 1,
	val periodTimespan: Timespan = Timespan.Month,
	val paymentMethod: String = "",
	
	val notes: String = "",
	val active: Boolean = true,
	
	@PrimaryKey(autoGenerate = true) val id: Int = 0
) : Serializable {
	
	constructor(model: Subscription) : this(
		name = model.name,
		description = model.description,
		color = model.color,
		
		amount = model.price,
		currency = model.currency,
		
		startedOn = model.startedOn?.toUtcMilliseconds(),
		periodAmount = model.period.length,
		periodTimespan = model.period.unit,
		paymentMethod = model.paymentMethod,
		notes = model.notes,
		active = model.isActive,
		id = model.id
	)
}
