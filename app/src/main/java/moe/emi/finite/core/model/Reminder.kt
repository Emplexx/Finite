package moe.emi.finite.core.model

import androidx.annotation.IntRange
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.Serializable as JavaSerializable
import java.util.Calendar

/**
 * @param remindInAdvance Period of time to notify in advance before the subscription day. If null, the reminder is on the same day as the billing date
 */
@Serializable
data class Reminder(
	val id: Int = 0,
	val subscriptionId: Int = 0,
	
	@SerialName("period")
	val remindInAdvance: Period?,
	
	@IntRange(0, 23) val hours: Int,
	@IntRange(0, 59) val minutes: Int,
) : JavaSerializable {
	
	init {
		require(hours in 0..23)
		require(minutes in 0..59)
	}
	
	companion object {
		
		fun empty(subscriptionId: Int): Reminder {
			
			val (hour, minute) = Calendar.getInstance().let {
				it.get(Calendar.HOUR_OF_DAY) to it.get(Calendar.MINUTE)
			}
			
			return Reminder(0, subscriptionId, null, hour, minute)
		}
	}
	
}