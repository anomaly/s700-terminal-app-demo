/**
 * TokenProvider.kt
 * Provides a Stripe Terminal Token from the server
 * @param coroutineScope The coroutine scope to use for the network request
 */
package ltd.anomaly.s700demo.terminal

import android.util.Log

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException

import com.fasterxml.jackson.databind.ObjectMapper

// Genereated API Client
import ltd.anomaly.s700demo.api.client.ApiStripeTerminalTokenClient
import okhttp3.OkHttpClient


/**
 * Provides a Stripe Terminal Token from the server
 * @param coroutineScope The coroutine scope to use for the network request
 */
class TokenProvider(private val coroutineScope: CoroutineScope) : ConnectionTokenProvider {

    val client = ApiStripeTerminalTokenClient(
        objectMapper = ObjectMapper(),
        baseUrl = "https://anomaly.ngrok.app",
        client = OkHttpClient()
    )

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val response = client.stripeTerminalTokenRetrieve()
                Log.d("TokenProvider","Token: ${response.data?.token}")
                callback.onSuccess(response.data?.token ?: "")
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onFailure(ConnectionTokenException("No token available"))
            }
        }
    }
}