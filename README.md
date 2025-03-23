# Stripe S700 Terminal Demo App

> [!WARNING]
> This repository is under heavily development, please wait for a release to be published along with relevant documentation for it to be useful

This repository contains the source for a demo Stripe Terminal on Device app designed to run on
their S700 Terminals. The aim of this project is to collect step by step learnings of how to
build an Android app for these devices (including wisdom on App SDK versions, target 
build platform), with a possible hope of recording a tutorial.

## Findings

Sections under here are what I found while I was trying to figure out both Android development
best practices and the particulars of S700 applications (read the section on [differences between
Standard Android](https://docs.stripe.com/terminal/features/apps-on-devices/overview#differences-from-standard-android).

## Checklist

The demo repository explores the following with docs and samples:

- [ ] Basic setup and build
- [ ] Additional library recommendations
- [ ] Stripe SDK working inside the app with UI widgets
- [ ] Network communication for application level interaction (REST calls)

### Operating Environment

S700 runs Android 10, so the easiest way to get started is to created an Android project with
a single Activity and ensure the project targets Android 10 (`Q`). Once you do this you will
likely get an error message pointing out that the `compileSdk` and `targetSdk` build settings
are incorrect and should be set to `35`. Change this in `build.gradle.kts` and the app
should happily build and run on the device.

Next following the instructions to [setup the Terminal SDK](https://docs.stripe.com/terminal/features/apps-on-devices/build).

First add the dependencies required for Terminal to work, note that you will have to get `gradle`
to download and sync the files.

```agsl
dependencies {
   implementation("com.stripe:stripeterminal-core:4.1.0")
   implementation("com.stripe:stripeterminal-handoffclient:4.1.0")
}
```
`Application` subclass to initialise the Terminal SDK.


Summary:
- Initialise using Android 10 project template with a single Activity
- Set `compileSdk` and `targetSdk` to `35`
- See [list of permissions automatically granted](https://docs.stripe.com/terminal/features/apps-on-devices/overview#android-permissions) to the app


## OpenApi3 Code Generation

I was interested in generating the client code for the API from the OpenAPI3 spec. Before I got to this I wanted to build a basic HTTP client and parse the JSON using dataclasses to understand how networking works and `OkHttp`.

The general premise is to be able to keep up to date with the API endpoints that the server side exposes (in our case written in Python).

### Manually written client

I wrote a very basic `dataclass` to parse the `TokenResponse` JSON object.

```kotlin
package ltd.anomaly.banjara.models

import kotlinx.serialization.Serializable

@Serializable
data class TokenResponse(val token: String)
```

And then used `HttpURLConnection` to make a GET request to the server and parse the JSON response. Note that I am using the `kotlinx.serialization` library to parse the JSON response (this is later removed from the project as I get Fabrikt to generate the client code).

```kotlin
package ltd.anomaly.banjara.network

// Experimental
import java.net.HttpURLConnection
import java.net.URL

import kotlinx.serialization.json.Json
import ltd.anomaly.banjara.models.TokenResponse


public object ManualApiClient {
    public fun getAccessToken(): String {
        // Launch a coroutine in the IO dispatcher for network calls
        try {
            val url = URL("https://localhost:8080/api/stripe/terminal/token/")
            println("URL: $url")
            with(url.openConnection() as HttpURLConnection) {
                requestMethod = "GET"  // optional default is GET

                // Read the response as text
                val jsonResponse = inputStream.bufferedReader().use { it.readText() }
                println("Response Code: $responseCode")
                println("Response Content: $jsonResponse")

                val obj = Json.decodeFromString<TokenResponse>(jsonResponse)
                println("Token: ${obj.token}")

                return obj.token

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}
```

Modify `build.gradle.kt` and add this to the `plugins` section:

```kotlin
plugins {
    ...
    kotlin("plugin.serialization") version "2.1.10"
}
```

You can now use this in the `MainActivity` to get the token from the server. Note the use of `coroutineScope` to launch the network call in the IO dispatcher.

```kotlin
Button(
    onClick = {
        coroutineScope.launch(Dispatchers.IO) {
            ManualApiClient.getAccessToken()
        }
    },
    colors = ButtonDefaults.buttonColors(

    )
) { Text("Call Alonnah") }
```

> While this is nice enough, the method doesn't scale and we can do a lot better and generate the code using OpenApi3 spec. This does require us to diligently name functions on the server side.
 
### Fabrikt

I found [Fabrikt](https://github.com/cjbooms/fabrikt?tab=readme-ov-file) which is maitnained by [Conor Gallagher](https://github.com/cjbooms), it also has [Gradle plugin](https://github.com/acanda/fabrikt-gradle-plugin) maintained by [Philip Graf](https://github.com/acanda). 

The documentation on the repositories are adequate, there were a few pieces missing for me as I am still a beginner with Gradle and Kotlin DSL.

First add the followign import at the top of your `gradle.build.kts` file (we will talk about it later):

```kotlin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
```

To add the plugin to your project, add the following to the `plugins` section of your `build.gradle.kts` file (make sure you sync projects with Gradle file):

```kotlin
plugins {
    ...
    id("ch.acanda.gradle.fabrikt") version "1.13.0"
}
```

You will be required the following dependencies in your `build.gradle.kts` file (the generated models files depend on `jackson` and `jakarta.validation`):

```kotlin
dependencies {
    ...
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("jakarta.validation:jakarta.validation-api:3.0.0")
}
```

Once you have done that, configure 'Fabrikt' in your `build.gradle.kts` file. Here is an example configuration:

```kotlin
fabrikt {
    generate("alonnah") {
        apiFile = file("src/main/openapi/alonnah.yaml")
        basePackage = "ltd.anomaly.banjara.api"
        client {
            generate = enabled
        }
    }
}
```

> Note that I have provided the package name `ltd.anomaly.banjara.api` and the path to the OpenApi3 spec file `src/main/openapi/alonnah.yaml`. The `client` generation is diabled by default, you can enable it by setting `generate = enabled`.

At this point you can run `./gradlew fabriktGenerate` to generate the client, however you can add the following `task` to generate the client before the compilation of the Kotlin code:

```kotlin
// Generate the client ahead of compilation
tasks.withType<KotlinCompile>().configureEach {
    dependsOn("fabriktGenerate")
}
```

Finally you have to add the generated sources to the `sourceSets` in the `build.gradle.kts` file:

```kotlin
android {
    ...
    sourceSets {
        getByName("main") {
            // For Android, you typically configure the java sources.
            java.srcDir(layout.buildDirectory.dir("generated/sources/fabrikt/src/main/kotlin").get().asFile)
        }
    }
}
```

This will allow you to use the generated client code in your project. The generated client code is in the `ltd.anomaly.banjara.api` package.

```kotlin
Button(
    onClick = {
        // Example of how to use the generated client
        coroutineScope.launch(Dispatchers.IO) {
            val client = ApiStripeTerminalTokenClient(
                objectMapper = ObjectMapper(),
                baseUrl = "https://localhost:8080",
                client = OkHttpClient()
            )
            try {
                val response = client.stripeTerminalTokenRetrieve()
                println("Token: ${response.data?.token}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    },
    colors = ButtonDefaults.buttonColors(

    )
) { Text("Call Alonnah") }
```

## Additional Libraries

- [OkHttp](https://square.github.io/okhttp/), HTTP client from Square (saw this in the Stripe sample app)
- [Jakarta validation], for validating input fields
- [fasterxml.jackson], for JSON parsing

## References

- [terminal-apps-on-deivce](https://github.com/stripe-samples/terminal-apps-on-devices), Github
repository containing a sample app from the Stripe Team
- [S700 Terminal Product Page](https://stripe.com/au/terminal/s700)

## License

Contents of this repository is licensed under the terms of the MIT License.
