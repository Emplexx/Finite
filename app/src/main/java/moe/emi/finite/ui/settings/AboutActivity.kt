package moe.emi.finite.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import moe.emi.finite.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
	lateinit var binding: ActivityAboutBinding
	override fun onCreate(savedInstanceState: Bundle?) {
		WindowCompat.setDecorFitsSystemWindows(window, false)
		super.onCreate(savedInstanceState)
		binding = ActivityAboutBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		
	}
}