package moe.emi.finite.service.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import moe.emi.finite.R
import moe.emi.finite.di.app
import moe.emi.finite.service.notifications.AlarmReceiver.Companion.goAsync
import java.net.URL

class BootReceiver : BroadcastReceiver() {
	
	lateinit var alarmScheduler: AlarmScheduler
	
	
	
	override fun onReceive(context: Context?, intent: Intent?) {
		
		
		
		PackageManager.DONT_KILL_APP
		Log.e("", "BootReceiver received intent ${intent?.action}")
		val notificationManager = context!!.getSystemService<NotificationManager>()!!
		notificationManager.notify(0, NotificationCompat.Builder(context, "Reminders").apply {
			setSmallIcon(R.drawable.ic_expand_more_24)
			setContentTitle("Boot completed")
		}.build())
		
		alarmScheduler = context.app?.container?.alarmScheduler ?: return
		return
		
//		context.getSystemService<NotificationManager>()!!.let {
//			it.notify(0, )
//		}
		
		Log.e("", "BootReceiver context check passed")
		
		
		
		if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
			
			Log.e("", "BootReceiver BOOT_COMPLETED check passed")
			
			goAsync {
				alarmScheduler.invalidateAllAlarms()
			}
		}
	}
}