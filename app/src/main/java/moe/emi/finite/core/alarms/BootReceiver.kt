package moe.emi.finite.core.alarms

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import moe.emi.finite.R
import moe.emi.finite.core.alarms.ReminderAlarmReceiver.Companion.REMINDER_CHANNEL_ID

class BootReceiver : BroadcastReceiver() {

	override fun onReceive(context: Context, intent: Intent) {
		
		// We're not calling invalidate all alarms here because it's already called in the app class
		if (intent.action == "android.intent.action.BOOT_COMPLETED") Log.d("Finite", "Boot completed")
		
		// TODO remove this later (before release)
		val notificationManager = context.getSystemService<NotificationManager>()!!
		notificationManager.notify(0, NotificationCompat.Builder(context, REMINDER_CHANNEL_ID).apply {
			setSmallIcon(R.drawable.ic_expand_more_24)
			setContentTitle("Boot completed")
		}.build())
		
	}
}