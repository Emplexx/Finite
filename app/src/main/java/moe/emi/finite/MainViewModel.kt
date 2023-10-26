package moe.emi.finite

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import moe.emi.finite.service.data.Rates
import moe.emi.finite.service.repo.RatesRepo
import moe.emi.finite.ui.details.Message
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(

) : ViewModel() {
	
	val messages = MutableLiveData<Message>(null)
	
}