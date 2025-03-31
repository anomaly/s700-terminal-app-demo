/**
 *
 */
package ltd.anomaly.s700demo.models

import android.util.Log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.stripeterminal.Terminal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.HandoffReaderListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.ConnectionStatus
import com.stripe.stripeterminal.external.models.DisconnectReason
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntentParameters
import com.stripe.stripeterminal.external.models.PaymentStatus
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.ReaderEvent
import com.stripe.stripeterminal.external.models.TerminalException

import okhttp3.OkHttpClient
import com.fasterxml.jackson.databind.ObjectMapper
import com.stripe.stripeterminal.external.models.CaptureMethod

import ltd.anomaly.s700demo.terminal.TokenProvider
import ltd.anomaly.s700demo.api.client.ApiStripeTerminalPaymentIntentClient


/**
 * A Model to represent the Checkout process
 *
 * @constructor Create empty Checkout model
 */
class CheckoutModel : ViewModel() {

    /**
     * Holds the Terminal token fetched from the server
     */
    val tokenProvider = TokenProvider(viewModelScope)

    /**
     * Holds the Stripe Terminal instance
     */
    private lateinit var targetReader: Reader

    /**
     * Cancellable discovery task,
     * this is used later on to cancel the task
     */
    private var discoveryTask: Cancelable? = null

    /**
     * Configuration required to find a Stripe Terminal reader
     * This is specific to the S700 hardware
     */
    private val discoveryConfiguration = DiscoveryConfiguration.HandoffDiscoveryConfiguration()

    /**
     * Holds the connection status of the terminal
     */
    private val _readerConnectStatus: MutableStateFlow<ConnectionStatus> = MutableStateFlow(
        ConnectionStatus.NOT_CONNECTED
    )

    private val _readerPaymentStatus: MutableStateFlow<PaymentStatus> = MutableStateFlow(
        PaymentStatus.NOT_READY
    )

    val readerConnectStatus: StateFlow<ConnectionStatus> = _readerConnectStatus.asStateFlow()

    val client = ApiStripeTerminalPaymentIntentClient(
        objectMapper = ObjectMapper(),
        baseUrl = "https://anomaly.ngrok.app",
        client = OkHttpClient()
    )

    fun processPaymentIntent() {
        // Get payment intent from the server by calling
        Log.d("CheckoutModel", "processPaymentIntent: ")
        // Create a local payment intent using Stripe Terminal API for 10 AUD
        val paymentIntentParams = PaymentIntentParameters.Builder()
            .setAmount(1000)
            .setCurrency("aud")
            .setCaptureMethod(CaptureMethod.Automatic)
            .build()

        Terminal.getInstance().createPaymentIntent(
            paymentIntentParams,
            object : PaymentIntentCallback {
                override fun onSuccess(paymentIntent: PaymentIntent) {
                    Log.d("CheckoutModel", "Payment intent created successfully")
                    val cancelable = Terminal.getInstance().collectPaymentMethod(
                        paymentIntent,
                        object : PaymentIntentCallback {
                            override fun onSuccess(paymentIntent: PaymentIntent) {
                                // Placeholder for handling successful operation
                                val cancelable = Terminal.getInstance().confirmPaymentIntent(
                                    paymentIntent,
                                    object : PaymentIntentCallback {
                                        override fun onSuccess(paymentIntent: PaymentIntent) {
                                            // Placeholder handling successful operation
                                        }

                                        override fun onFailure(e: TerminalException) {
                                            // Placeholder for handling exception
                                        }
                                    }
                                )
                            }

                            override fun onFailure(e: TerminalException) {
                                // Placeholder for handling exception
                            }
                        }
                    )
                }

                override fun onFailure(e: TerminalException) {
                    Log.e("CheckoutModel", "Failed to create payment intent: ${e.errorMessage}")
                }
            }
        )

    }

    // region Wrappers and helpers
    @Suppress("MissingPermission") // required permissions are handled in the app - S700
    fun findLocalReaders() {
        discoveryTask = Terminal.getInstance().discoverReaders(
            discoveryConfiguration,
            stripeReaderDiscoveryListener,
            stripeReaderDiscoveryCallback
        )
    }

    fun connectReader() {
        getCurrentReader()?.let { reader ->
            // same one , skip
            if (targetReader.id == reader.id) {
                return
            }

            // different reader , disconnect old first then connect new one again
            val currentReader: Reader = reader
            Terminal.getInstance().disconnectReader(object : Callback {
                override fun onSuccess() {
                    Log.d("ConnectReader", "Current Reader [ ${currentReader.id} ] disconnect success")
                }

                override fun onFailure(e: TerminalException) {
                    Log.e("ConnectReader", "Current Reader [ ${currentReader.id} ] disconnect fail")
                }
            })
        }

        Log.i("ConnectReader", "Connecting to new Reader [ ${targetReader.id} ] .... ")
        val readerCallback: ReaderCallback = object : ReaderCallback {
            override fun onSuccess(reader: Reader) {
                Log.i("ConnectReader", "Reader [ ${targetReader.id} ] Connected ")
            }

            override fun onFailure(e: TerminalException) {
                //_userMessage.update { e.errorMessage }
            }
        }

        Terminal.getInstance().connectReader(
            targetReader,
            ConnectionConfiguration.HandoffConnectionConfiguration(
                object : HandoffReaderListener {
                    override fun onDisconnect(reason: DisconnectReason) {
                        Log.i("ConnectReader", "onDisconnect: $reason")
                    }

                    override fun onReportReaderEvent(event: ReaderEvent) {
                        Log.i("ConnectReader", "onReportReaderEvent: $event")
                    }
                }
            ),
            readerCallback
        )
    }
    // endregion

    // region Reader Discovery

    private val stripeReaderDiscoveryCallback: Callback = object : Callback {
        override fun onSuccess() {
            // Handle success
            Log.d("StripeDiscoveryCallback", "discoveryCallback onSuccess")
        }

        override fun onFailure(e: TerminalException) {
            // Handle failure
        }
    }

    private val stripeReaderDiscoveryListener: DiscoveryListener = object : DiscoveryListener {
        override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
            val reader = readers.firstOrNull { it.networkStatus == Reader.NetworkStatus.ONLINE }
            if (reader != null) {
                targetReader = reader
                connectReader()
            } else {
                Log.d("StripeDiscoveryListener", "No reader found")
            }
        }
    }

    // endregion

    // region State accessors and mutators
    private fun getCurrentReader(): Reader? {
        return Terminal.getInstance().connectedReader
    }

    // endregion
}