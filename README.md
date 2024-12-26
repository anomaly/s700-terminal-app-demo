# Stripe S700 Terminal Demo App

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

### Additional Libraries

- [OkHttp](https://square.github.io/okhttp/), HTTP client from Square (saw this in the Stripe sample app)

## References

- [terminal-apps-on-deivce](https://github.com/stripe-samples/terminal-apps-on-devices), Github
repository containing a sample app from the Stripe Team
- [S700 Terminal Product Page](https://stripe.com/au/terminal/s700)

## License

Contents of this repository is licensed under the terms of the MIT License.
