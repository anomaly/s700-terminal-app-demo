/**
 * Event listen er for Stripe Terminal SDK
 *
 * This is a singleton object that implements the TerminalListener interface.
 * See MainActivity.kt for usage.
 */
package ltd.anomaly.s700demo.terminal

import android.util.Log

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.Flow

import com.stripe.stripeterminal.external.callable.TerminalListener
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.PaymentStatus

/**
 * A listener for Stripe terminal events
 */
object TerminalEventListener : TerminalListener {

    private val _onConnectionStatusChange = MutableSharedFlow<ConnectionStatus>()
    private val _onPaymentStatusChange = MutableSharedFlow<PaymentStatus>()

    val onConnectionStatusChange: Flow<ConnectionStatus> = _onConnectionStatusChange.asSharedFlow()
    val onPaymentStatusChange: Flow<PaymentStatus> = _onPaymentStatusChange.asSharedFlow()

    override fun onConnectionStatusChange(status: ConnectionStatus) {
        Log.d("TerminalEventListener", "Connection status changed: $status")
        _onConnectionStatusChange.tryEmit(status)
    }

    override fun onPaymentStatusChange(status: PaymentStatus) {
        Log.d("TerminalEventListener", "Payment status changed: $status")
        _onPaymentStatusChange.tryEmit(status)
    }

}