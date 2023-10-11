package dev.vstd.myiot.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.vstd.myiot.data.LedHistoryModel
import dev.vstd.myiot.data.RemoteCenter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainVimel : ViewModel() {
    companion object {
        const val TAG = "MainVimel"
    }
    private val remoteCenter = RemoteCenter()

    val rawMessage = MutableStateFlow<List<Pair<Sender, String>>>(emptyList())
    val uiState = MutableStateFlow(UiState())

    init {
        remoteCenter.start()
        viewModelScope.launch {
            launch {
                remoteCenter.dataFlow.collect {
                    rawMessage.value += Sender.FIREBASE to it
                    val sensorData = SensorModel.fromJson(it)
                    uiState.value = uiState.value.copy(
                        temperature = sensorData.temp,
                        humidity = sensorData.humid,
                        lux = sensorData.lux,
                        time = sensorData.seconds,
                    )
                }
            }
            launch {
                remoteCenter.ledFlow.collect {
                    rawMessage.value += Sender.USER to it
                    val ledHistory = LedHistoryModel.fromJson(it)
                    when (ledHistory.which) {
                        1 -> {
                            uiState.value = uiState.value.copy(led1On = ledHistory.on)
                        }
                        2 -> {
                            uiState.value = uiState.value.copy(led2On = ledHistory.on)
                        }
                    }
                }
            }
        }
    }

    fun toggleLed(which: Int, boolean: Boolean) {
        remoteCenter.toggleLed(which, boolean)
        viewModelScope.launch {
            rawMessage.value += Sender.USER to "Toggle LED$which: $boolean"
        }
    }

    enum class Sender {
        FIREBASE, USER
    }

    override fun onCleared() {
        super.onCleared()
        remoteCenter.destroy()
    }

    data class UiState(
        val temperature: Float = 0f,
        val humidity: Float = 0f,
        val lux: Float = 0f,
        val time: Long = 0L,
        val led1On: Boolean = false,
        val led2On: Boolean = false,
    )
}