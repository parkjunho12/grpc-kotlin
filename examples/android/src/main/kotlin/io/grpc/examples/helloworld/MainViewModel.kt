package io.grpc.examples.helloworld

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    private val _chatLiveData = MutableLiveData<String>()
    val chatLiveData: LiveData<String> get() = _chatLiveData

    fun sendChat(chat: String) {
        viewModelScope.launch {

        }
    }

}