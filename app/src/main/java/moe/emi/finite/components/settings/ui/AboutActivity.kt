package moe.emi.finite.components.settings.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import dev.chrisbanes.insetter.applyInsetter
import moe.emi.finite.BuildConfig
import moe.emi.finite.FiniteApp
import moe.emi.finite.R
import moe.emi.finite.components.upgrade.UpgradeSheet
import moe.emi.finite.databinding.ActivityAboutSlayBinding
import moe.emi.finite.dump.collectOn
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

class AboutActivity : AppCompatActivity() {
	
	lateinit var binding: ActivityAboutSlayBinding
	
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		
		binding = ActivityAboutSlayBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.root.applyInsetter {
			type(navigationBars = true) {
				margin()
			}
		}
		
		binding.textVersion.text = buildString {
			append(BuildConfig.VERSION_NAME)
			append("    /    ")
			append(getBuildDate())
		}
		
		binding.rowUpgrade.apply {
			icon.setImageResource(R.drawable.ic_upgrade_24)
			textLabel.text = "Upgrade"
			root.setOnClickListener {
				UpgradeSheet().show(supportFragmentManager, null)
			}
		}
		binding.rowThanks.apply {
			// TODO tint icon and text pink
			icon.setImageResource(R.drawable.ic_favorite_24)
			textLabel.text = "Thank you!"
			root.setOnClickListener {
				binding.konfettiView.start(rain())
			}
		}
		
		binding.rowPlayStore.apply {
			textLabel.text = "Play Store"
			textValue.isVisible = false
			root.setOnClickListener {
				openUrl("https://play.google.com/store/apps/details?id=moe.emi.finite")
			}
		}
		binding.rowLicenses.apply {
			textLabel.text = "Open-Source Licenses"
			textValue.isVisible = false
			root.setOnClickListener {
				startActivity(Intent(this@AboutActivity, OssLicensesMenuActivity::class.java))
			}
		}
		
		binding.headerDeveloper.text.text = "Created by"
		
		binding.buttonWebsite.setOnClickListener {
			openUrl("https://emi.moe")
		}
		binding.buttonBluesky.setOnClickListener {
			openUrl("https://bsky.app/profile/emplexx.emi.moe")
		}
		
		FiniteApp.instance.container.upgradeState.collectOn(this) {
			
			if (!BuildConfig.DEBUG) {
				binding.sectionUpgrade.isVisible = !it.isPro
				binding.sectionThanks.isVisible = it.isPro
			}
		}
	}
	
	override fun onResume() {
		super.onResume()
		
		// If activity was put into background, the animation start degree needs to be recalculated.
		// This is why we're using onResume
		animate()
	}
	
	private fun animate() {
		
		binding.finiteCircle.clearAnimation()
		
		val time = System.currentTimeMillis()
		val fractionSecondPassed = (time % 60_000.0) / 60_000
		val degreesPassed = 360.0 * fractionSecondPassed
		val degOffset = degreesPassed.toFloat() - 90f
		
		RotateAnimation(
			0f + degOffset,
			360f + degOffset,
			RotateAnimation.RELATIVE_TO_SELF,
			0.5f,
			RotateAnimation.RELATIVE_TO_SELF,
			0.5f
		).apply {
			repeatMode = RotateAnimation.RESTART
			repeatCount = RotateAnimation.INFINITE
			duration = 60 * 1000
			interpolator = LinearInterpolator()
		}.let {
			binding.finiteCircle.startAnimation(it)
		}
		
	}
	
	private fun getBuildDate(): String {
		val devTimeZone = ZoneId.of("Europe/Warsaw")
		val dateTime = LocalDateTime.ofInstant(BuildConfig.BUILD_TIME.toInstant(), devTimeZone)
		return DateTimeFormatter
			.ofLocalizedDate(FormatStyle.MEDIUM)
			.format(dateTime.toLocalDate())
	}
	
	private fun Context.openUrl(url: String) {
		val url1 = url.let {
			if (!it.startsWith("http://") && !it.startsWith("https://")) "http://$it"
			else it
		}
		startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url1)))
	}
	
	private fun rain(): List<Party> {
		return listOf(
			Party(
				speed = 0f,
				maxSpeed = 15f,
				damping = 0.9f,
				angle = Angle.BOTTOM,
				spread = Spread.ROUND,
				colors = listOf(0xfce18a, 0xff726d, 0xf4306d, 0xb48def),
				emitter = Emitter(duration = 2, TimeUnit.SECONDS).perSecond(100),
				position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
			)
		)
	}
	
}