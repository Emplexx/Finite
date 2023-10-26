package moe.emi.finite.ui.details

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.emi.finite.service.repo.SubscriptionsRepo
import java.util.concurrent.Flow.Subscription
import javax.inject.Inject

@HiltViewModel
class SubscriptionDetailsViewModel @Inject constructor(
	savedState: SavedStateHandle,
) : ViewModel() {
	
	val entityId = savedState.get<Int>("ID")!!
	val subscription = SubscriptionsRepo.getSubscription(entityId).asLiveData()

	val messages = MutableLiveData<Message>(null)
	
	fun pauseSubscription() = viewModelScope.launch {
		SubscriptionsRepo.getSubscription(entityId).first()?.let {
			SubscriptionsRepo.pauseSubscription(it.id, it.active)
			
			messages.postValue(
				Message(
					key = if (it.active) "Paused" else "Resumed",
				)
			)
		}
	}
	
	fun deleteSubscription() = viewModelScope.launch {
		SubscriptionsRepo.deleteSubscription(entityId)
			.let {
				messages.postValue(
					Message(
						if (it.isSuccess) "Delete" else "Error"
					)
				)
				
			}
	}
	
}