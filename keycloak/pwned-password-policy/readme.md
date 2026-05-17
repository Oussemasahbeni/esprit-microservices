# Keycloak Pwned Password Policy Provider

A custom Keycloak SPI (Service Provider Interface) that prevents users from using passwords that have been exposed in
known data breaches. This provider integrates with the **Have I Been Pwned (HIBP)** Pwned Passwords API to enhance your
application's security.

It uses the **k-Anonymity** model, which ensures that a user's actual password is never transmitted over the internet,
preserving user privacy while checking for breaches.

## Table of Contents

- [Features](#features)
- [How It Works (K-Anonymity)](#how-it-works-k-anonymity)
- [Prerequisites](#prerequisites)
- [Installation and Deployment](#installation-and-deployment)
- [Configuration in Keycloak](#configuration-in-keycloak)
- [Internationalization (i18n)](#internationalization-i18n)
- [Building the Project](#building-the-project)
- [License](#license)

## Features

- **Prevent Pwned Passwords**: Rejects passwords that are known to be compromised.
- **Privacy-Preserving**: Uses the HIBP k-Anonymity model. The full password or hash is never sent to any external
  service.
- **Configurable Breach Threshold**: As an admin, you can set a threshold for how many times a password can appear in
  breaches before it's rejected (e.g., reject if found 1 or more times).
- **Internationalized Error Messages**: Provides clear error keys for easy translation via Keycloak themes.
- **Fail-Closed Design**: If the HIBP API is unreachable, the policy will reject the password by default to maintain
  security (this is a secure default).

## How It Works (K-Anonymity)

The provider follows these steps to check a password without exposing it:

1. **Hash**: The user's chosen password is hashed locally using SHA-1.
2. **Prefix**: The first 5 characters of the SHA-1 hash are extracted.
3. **API Query**: This 5-character prefix is sent to the HIBP API (`https://api.pwnedpasswords.com/range/{prefix}`).
4. **Receive Suffixes**: The API responds with a list of all hash *suffixes* that start with that prefix, along with
   their breach counts.
5. **Local Check**: The provider checks locally if the remainder of the original hash (the suffix) exists in the list
   returned by the API.
6. **Enforce Policy**: If a match is found, the password has been pwned, and the policy rejects it. If no match is
   found, the password is considered safe by this policy.

## Prerequisites

- Java 21 or higher
- Apache Maven 3.6+
- A running Keycloak instance (v26+ recommended).

## Installation and Deployment

Follow these steps to build the provider and deploy it to your Keycloak server.

**1. Build the Provider JAR**

Clone this repository and run the following Maven command from the project's root directory:

```bash
mvn clean package
```

This will create a JAR file named `pwned-password-policy-1.0.jar` (or similar) in the `target/` directory.

**2. Deploy the JAR to Keycloak**

Copy the generated JAR file to Keycloak's providers directory. The location depends on your Keycloak distribution.

Copy the JAR to the `providers` directory.

  ```bash
  cp target/pwned-password-policy-1.0.jar /path/to/keycloak/providers/
  ```

**3. Build/Restart Keycloak**

- **For Keycloak (Quarkus Distribution)**:
  You must run the `build` command for Keycloak to recognize the new provider.
  ```bash
  # Navigate to your Keycloak bin directory
  cd /path/to/keycloak/bin/

  # Run the build command
  ./kc.sh build
  ```
  After the build is complete, restart your Keycloak server.

- **For Keycloak (WildFly/Legacy Distribution)**:
  Simply restart the Keycloak server. The provider will be hot-deployed.

## Configuration in Keycloak

Once the provider is deployed, you can enable it for any realm.

1. Log in to the **Keycloak Admin Console**.
2. Select your desired realm from the top-left dropdown.
3. Navigate to **Authentication** in the left-hand menu.
4. Click on the **Password Policy** tab.
5. In the **Add policy** dropdown menu, you will see a new option: **Pwned Password**. Select it.
6. A new policy will be added to the list. You can set the **Policy Value**:
    - This integer represents the **breach threshold**. A password that has appeared in `n` or more breaches will be
      rejected.
    - A recommended value is `1` (reject if found even once).
7. Click **Save**.

The policy is now active for your realm!

## Internationalization (i18n)

The provider uses message keys for error messages, allowing them to be translated in a custom theme.

**Error Keys:**

- `pwnedPasswordError`: The primary error shown to users when their password is found in a breach.
- `pwnedPasswordApiError`: Shown if the HIBP API cannot be reached.
- `pwnedPasswordInternalError`: Shown for any other unexpected server-side error.

To add translations, create a custom theme and add the keys to your `messages/messages_{locale}.properties` files.

**Example for `messages_en.properties`:**

```properties
pwnedPasswordError=This password has been exposed in a data breach and cannot be used. Please choose a different password.
pwnedPasswordApiError=Could not verify password security. Please try again later.
pwnedPasswordInternalError=An internal error occurred while validating the password. Please contact supp
```

**Example for `messages_fr.properties`:**

```properties
pwnedPasswordError=Ce mot de passe a été compromis lors d'une fuite de données et ne peut être utilisé. Veuillez choisir un autre mot de passe.
pwnedPasswordApiError=Impossible de vérifier la sécurité du mot de passe. Veuillez réessayer plus tard.
pwnedPasswordInternalError=Une erreur interne est survenue lors de la validation du mot de passe. Veuillez contacter le support.
```

## Building the Project

Ensure the `<keycloak.version>` property in your `pom.xml` matches the version of Keycloak you are running.

```xml

<properties>
    <keycloak.version>26.2.5</keycloak.version>
</properties>
```

Then, build with:

```bash
mvn clean package
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.