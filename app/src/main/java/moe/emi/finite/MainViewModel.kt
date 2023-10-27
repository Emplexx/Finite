package moe.emi.finite

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import moe.emi.finite.ui.details.Event
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

) : ViewModel() {
	
	val messages = MutableLiveData<Event>(null)
	
}