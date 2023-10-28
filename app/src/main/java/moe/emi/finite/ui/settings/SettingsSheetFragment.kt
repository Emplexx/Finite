package moe.emi.finite.ui.settings

import android.app.Dialog
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsSheetFragment : BottomSheetDialogFragment() {
	
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return SettingsSheet(requireContext())
	}
}