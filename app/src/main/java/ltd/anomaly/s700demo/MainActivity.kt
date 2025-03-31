package ltd.anomaly.s700demo

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.LocalContext

import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ButtonDefaults

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope

import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel

import ltd.anomaly.s700demo.ui.theme.S700DemoTheme
import ltd.anomaly.s700demo.models.CheckoutModel

import ltd.anomaly.s700demo.terminal.TerminalEventListener

class MainActivity : ComponentActivity() {

    // Don't put this in the composeable view
    val checkoutModel: CheckoutModel by viewModels<CheckoutModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Terminal.initTerminal(
            applicationContext,
            LogLevel.VERBOSE,
            checkoutModel.tokenProvider,
            TerminalEventListener // see TerminalEventListener.kt
        )
        // Discover readers
        // Move this to a task
        checkoutModel.findLocalReaders()

        setContent {
            BanjaraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    val coroutineScope = rememberCoroutineScope()

    // Access the checkout model here
    val checkoutModel: CheckoutModel = (LocalContext.current as MainActivity).checkoutModel

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )

        Button(onClick = {
            // Charge a card
            println("Charge card")
            checkoutModel.processPaymentIntent()
        }) {
            Text("Charge Card")
        }
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BanjaraTheme {
        Greeting("Android")
    }
}