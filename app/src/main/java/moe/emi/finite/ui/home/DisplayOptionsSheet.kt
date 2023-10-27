package moe.emi.finite.ui.home

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import dev.chrisbanes.insetter.applyInsetter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.convenience.TonalColor
import moe.emi.convenience.materialColor
import moe.emi.finite.FiniteApp
import moe.emi.finite.databinding.LayoutSheetDisplayBinding
import moe.emi.finite.databinding.LayoutSortRowBinding
import moe.emi.finite.dump.FastOutExtraSlowInInterpolator
import moe.emi.finite.dump.getStorable
import moe.emi.finite.dump.visible
import moe.emi.finite.service.datastore.AppSettings
import moe.emi.finite.service.datastore.appSettings
import moe.emi.finite.service.datastore.storeGeneral
import moe.emi.finite.set

class DisplayOptionsSheet(
	context: Context,
//	val onSelect: (Sort) -> Unit,
//	val onAsc: (Boolean) -> Unit,
) : BottomSheetDialog(context) {
	
	private lateinit var binding: LayoutSheetDisplayBinding
	private var selectedRow: LayoutSortRowBinding? = null
	private var isAscending = true
	
	data class SortSpec(
		val ascending: String,
		val descending: String,
	)
	
	override fun onStart() {
		super.onStart()
		
		binding = LayoutSheetDisplayBinding.inflate(layoutInflater)
		setContentView(binding.root)
		
		binding.scrollView.applyInsetter {
			type(navigationBars = true) {
				padding(bottom = true)
			}
		}
		
		lifecycleScope.launch { initLayout() }
	}
	
	private suspend fun initLayout() {
		// TODO string res
		binding.rowName.textLabel.text = "Name"
		binding.rowPrice.textLabel.text = "Price"
		binding.rowDate.textLabel.text = "Next billing date"
		
		selectedRow = when (context.appSettings.first().sort) {
			Sort.Alphabetical -> binding.rowName
			Sort.Price -> binding.rowPrice
			Sort.Date -> binding.rowDate
		}
		selectedRow?.select()
		isAscending = context.appSettings.first().sortIsAscending
		
		listOf(
			binding.rowName,
			binding.rowPrice,
			binding.rowDate,
		).forEach {
			it.root.setOnClickListener { _ ->
				
				if (selectedRow == it) {
					selectedRow?.toggle()
					
					lifecycleScope.launch {
						val ascending = isAscending
						context.appSettings.first().copy(sortIsAscending = !ascending).set()
						isAscending = !ascending
					}
				}
				else {
					selectedRow?.unselect()
					selectedRow = it
					selectedRow?.select()
					
					when (it) {
						binding.rowName -> Sort.Alphabetical
						binding.rowPrice -> Sort.Price
						binding.rowDate -> Sort.Date
						else -> null
					}?.let { lifecycleScope.launch {
						context.appSettings.first().copy(sort = it).set()
					} }
				}
				
				
			}
		}
	}
	
	private var jobReturnArrow: Job? = null
	private fun LayoutSortRowBinding.toggle() {
		jobReturnArrow?.cancel()
		lifecycleScope.launch {
			
			trailingIcon.animate()
				.setInterpolator(FastOutExtraSlowInInterpolator())
				.setDuration(500)
				.rotationBy(180f)
			
			jobReturnArrow = launch {
				delay(1000)
				val asc = context.appSettings.first().sortIsAscending
				val rem = trailingIcon.rotation.toInt() % 180
				if (rem != 0) {
					
					if (trailingIcon.rotation > 360f * 2) {
						trailingIcon.rotation = trailingIcon.rotation % 360f
					}
					
					trailingIcon.animate()
						.setInterpolator(FastOutExtraSlowInInterpolator())
						.setDuration(500)
						.rotation(if (asc) 0f else 180f)
				}
			}
		}
	}
	
	private fun LayoutSortRowBinding.select() {
		lifecycleScope.launch {
			with(root.context) {
				
				root.setBackgroundColor(materialColor(TonalColor.primaryContainer))
				textLabel.setTextColor(materialColor(TonalColor.onPrimaryContainer))
				textHint.children.forEach {
					(it as? TextView)?.setTextColor(materialColor(TonalColor.onPrimaryContainer))
				}
			}
			
			val asc = context.appSettings.first().sortIsAscending
			Log.d("TAG", "asc $asc")
			trailingIcon.rotation = if (asc) 0f else 180f
			trailingIcon.isSelected = asc
			
			trailingIcon.visible = true
		}
	}
	
	private fun LayoutSortRowBinding.unselect() {
		with(root.context) {
			
			root.setBackgroundColor(Color.TRANSPARENT)
			textLabel.setTextColor(materialColor(TonalColor.onSurface))
			textHint.children.forEach {
				(it as? TextView)?.setTextColor(materialColor(TonalColor.onSurface))
			}
			trailingIcon.visible = false
			
		}
	}
}