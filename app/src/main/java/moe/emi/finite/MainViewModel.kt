package moe.emi.finite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.emi.finite.ui.details.Event
import javax.inject.Inject

class MainViewModel @Inject constructor(

) : ViewModel() {
	
	val messages = MutableLiveData<Event>(null)
	
}