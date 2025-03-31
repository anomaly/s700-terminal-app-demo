package ltd.anomaly.s700demo

import android.app.Application
import android.util.Log
import com.stripe.stripeterminal.TerminalApplicationDelegate

class S700DemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Print a message to the log
        TerminalApplicationDelegate.onCreate(this)
    }
}