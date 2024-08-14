# Sudo DI Edge Agent Sample App for Android

## Overview
This project provides examples for how to use the Sudo DI Edge Agent Android SDK.

## Supported Android Versions
## Version Support
| Technology             | Supported version |
|------------------------|-------------------|
| Min. Deployment Target | 26                |
| Kotlin version         | 1.9+              |

## Getting Started

To build this app you first need to obtain an SDK configuration file.

1. Contact the Sudo Platform for access to the Edge Agent SDK [partners@sudoplatform.com](mailto:partners@sudoplatform.com).

2. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain an SDK configuration file (sudoplatformconfig.json)

3. Add the SDK configuration file to the project in the following location:
   
   Add the config file `sudoplatformconfig.json` to the assets folder of the app module.
   
   If the application does not already have an assets folder, create one under `app/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

4. Apply the steps to configure the TEST registration method below.

5. Optionally, the application can be configured to use a different DI "ledger" by replacing the `app/src/main/assets/ledger_genesis.json` file with the genesis file of the ledger you wish to use. By default, this sample app is using the indicio testnet, however you may need to change this if you're testing an DI ecosystem with a different ledger.

6. Build the app
 
7. Run the app on an emulator (AVD) or Android device running Android 8 (API level 26) or later that is not rooted and does not have an unlocked bootloader.

### TEST Registration Set Up

To build and use this app with TEST registration, you will need a TEST registration private key and key identifier to add them to the project.

1. Follow the steps in the [Getting Started guide](https://docs.sudoplatform.com/guides/getting-started) and in [User Registration](https://docs.sudoplatform.com/guides/users/registration) to obtain a TEST registration private key and TEST registration key identifier.

2. Add the TEST registration private key and TEST registration key identifier to the project in the following locations:

   Add the private key file `register_key.private` to the assets folder of the app module.
   Add a text file `register_key.id` containing the test registration key ID.

   If the application does not already have an assets folder, create one under `app/src/main/assets/` or in Android Studio, right click on the application module and select `New > Folder > Assets Folder`. Then drag the files to that folder.

## More Documentation
Refer to the following documents for more information:
* [Sudo DI Edge Agent SDK Docs](https://docs.sudoplatform.com/guides/decentralized-identity/decentralized-identity/edge-agent-sdk)
* [Getting Started on Sudo Platform](https://docs.sudoplatform.com/guides/getting-started)
* [Understanding Sudo Digital Identities](https://docs.sudoplatform.com/concepts/sudo-digital-identities)

## Issues and Support
File issues you find with this sample app in this Github repository. Ensure that you do not include any Personally Identifiable Information(PII), API keys, custom endpoints, etc. when reporting an issue.

For general questions about the Sudo Platform, please contact [partners@sudoplatform.com](mailto:partners@sudoplatform.com)
