package moe.emi.finite.ui.editor

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColor
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import codes.side.andcolorpicker.converter.setFromColorInt
import codes.side.andcolorpicker.converter.toColorInt
import codes.side.andcolorpicker.group.PickerGroup
import codes.side.andcolorpicker.group.registerPickers
import codes.side.andcolorpicker.hsl.HSLColorPickerSeekBar
import codes.side.andcolorpicker.model.Color
import codes.side.andcolorpicker.model.IntegerHSLColor
import codes.side.andcolorpicker.view.picker.ColorSeekBar
import codes.side.andcolorpicker.view.picker.ColorSeekBar.DefaultOnColorPickListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.emi.finite.databinding.LayoutDialogColorPickerBinding

fun Context.showColorPickerDialog(
	color: Int,
	dialogListener: (Int) -> Unit): AlertDialog {
	
	val binding = LayoutDialogColorPickerBinding.inflate(LayoutInflater.from(this))
	
	val group = PickerGroup<IntegerHSLColor>().apply {
		registerPickers(binding.hueSeekBar, binding.lightnessSeekBar, binding.saturationSeekBar)
	}
	
	fun setSelectedColor(color: Int) {
		IntegerHSLColor().also {
			it.setFromColorInt(color)
			binding.swatchView.setSwatchColor(it)
			group.setColor(it)
		}
	}
	
	var pickedColor = color
	
	val listener = object : DefaultOnColorPickListener<ColorSeekBar<IntegerHSLColor>, IntegerHSLColor>() {
		override fun onColorPicking(
			picker: ColorSeekBar<IntegerHSLColor>,
			color: IntegerHSLColor,
			value: Int,
			fromUser: Boolean
		) {
			binding.fieldHex.clearFocus()
			binding.swatchView.setSwatchColor(color)
			pickedColor = color.toColorInt()
			binding.fieldHex.setText(String.format("%06X", 0xFFFFFF and color.toColorInt()))
		}
	}
	group.addListener(listener)
	
	binding.fieldHex.addTextChangedListener(
		beforeTextChanged = { text, start, count, after ->
			Log.d("beforeTextChanged", "$text, $start, $count, $after")
		},
		onTextChanged = { text, start, before, count ->
			
			Log.d("onTextChanged", "$text, $start, $before, $count")
		},
		afterTextChanged = {
			Log.d("afterTextChanged", "$it")
		}
	)
	binding.fieldHex.doAfterTextChanged { text ->
		text ?: return@doAfterTextChanged
		val formattedString = text
			.let { if (it.getOrNull(0) == '#') it.drop(1) else it } // Remove leading #
			.let { if (it.length > 6) it.substring(0, 6) else it } // Trim to 6 chars
		
		if (text != formattedString) {
			binding.fieldHex.setText(formattedString)
			binding.fieldHex.setSelection(formattedString.length)
		}
		
		if (binding.fieldHex.hasFocus()) {
			formattedString.runCatching { android.graphics.Color.parseColor("#$this") }
				.getOrNull()
				?.let(::setSelectedColor)
		}
	}
	
	setSelectedColor(color)
	
	val dialog = MaterialAlertDialogBuilder(this)
		.setView(binding.root)
		.setPositiveButton("OK") { dialog, _ ->
			dialog.dismiss()
			dialogListener(pickedColor)
		}
		.create()
		.also { it.show() }
	
	return dialog
}