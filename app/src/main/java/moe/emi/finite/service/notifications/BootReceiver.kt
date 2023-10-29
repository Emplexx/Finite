package moe.emi.finite.service.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import moe.emi.finite.service.notifications.AlarmReceiver.Companion.goAsync
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
	
	@Inject
	lateinit var alarmScheduler: AlarmScheduler
	
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
			goAsync {
				alarmScheduler.invalidateAllAlarms()
			}
		}
	}
}