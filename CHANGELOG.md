# 1.2.0 / 2020-01-20

### Changes

* [BUGFIX] Fail silently when trying to initialize the SDK twice. See #86 (Thanks @Vavassor)
* [BUGFIX] Publish the Timber artifact automatically. See #90 (Thanks @Macarse)
* [FEATURE] Create a Crash Handler : App crashes will be automatically logged.
* [FEATURE] Downgrade OkHttp4 to OkHttp3
* [FEATURE] Make Library compatible with API 19+
* [FEATURE] Trigger background upload when the app is used offline
* [FEATURE] Use DownloadSpeed and signal strength to add info on connectivity
* [FEATURE] Use Gzip for log upload requests
* [OTHER] Analyse Benchmark reports in the CI
* [OTHER] Fix the flaky test in DataDogTimeProviderTest
* [OTHER] Generate a report on the SDK API changes ([dd-sdk-android/apiSurface](dd-sdk-android/apiSurface))

# 1.1.1 / 2020-01-07

### Changes

* [BUGFIX] Fix crash on Android Lollipop and Nougat

# 1.1.0 / 2020-01-06

### Changes

* [BUGFIX] Make the packageVersion field optional in the SDK initialisation
* [BUGFIX] Fix timestamp formatting in logs
* [FEATURE] Add a developer targeted logger 
* [FEATURE] Add user info in logs
* [FEATURE] Create automatic Tags / Attribute (app / sdk version)
* [FEATURE] Integrate SDK with Timber
* [IMPROVEMENT] Remove the obfuscation in the logs (faster local processing)
* [IMPROVEMENT] Implement a modern NetworkInfoProvider

# 1.0.0 / 2019-12-17

### Changes

* [BUGFIX] Make sure no logs are lost
* [FEATURE] Allow adding a throwable to logged messages
* [FEATURE] Automatically add the Logger name and thread to the logged messages
* [FEATURE] Allow Changing the endpoint between US and EU servers
* [FEATURE] Allow logger to add attributes for a single log
* [IMPROVEMENT] Remove the usage of the Android WorkManager
* [IMPROVEMENT] Improved overall performances
* [IMPROVEMENT] Disable background job when network or battery are low
* [OTHER] Secure the project with lints, tests and benchmarks