package com.oussema;

import static com.oussema.PwnedPasswordPolicyProviderFactory.ID;
import static java.net.http.HttpClient.newHttpClient;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.stream.Stream;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PolicyError;

/**
 * A Keycloak password policy provider that checks if a password has been exposed in a data breach
 * using the "Have I Been Pwned" (HIBP) API.
 */
public class PwnedPasswordPolicyProvider implements PasswordPolicyProvider {

  private static final String HIBP_API_URL = "https://api.pwnedpasswords.com/range/";
  private final KeycloakContext context;

  public PwnedPasswordPolicyProvider(KeycloakContext context) {
    this.context = context;
  }

  @Override
  public PolicyError validate(RealmModel realm, UserModel user, String password) {
    return validate(password);
  }

  @Override
  public PolicyError validate(String s, String password) {
    return validate(password);
  }

  private PolicyError validate(String password) {
    try {
      // Calculate SHA-1 hash of the password
      MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
      byte[] hashBytes = sha1.digest(password.getBytes(UTF_8));

      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        hexString.append(String.format("%02X", b));
      }
      String hash = hexString.toString();

      // Split hash into prefix (5 chars) and suffix
      String prefix = hash.substring(0, 5);
      String suffix = hash.substring(5);

      // Query the HIBP API
      HttpRequest request = HttpRequest.newBuilder().uri(URI.create(HIBP_API_URL + prefix)).build();

      try (HttpClient client = newHttpClient()) {
        HttpResponse<Stream<String>> response =
                client.send(request, HttpResponse.BodyHandlers.ofLines());

        if (response.statusCode() != 200) {
          return null;
        }

        // Check if our hash suffix is in the response
        long breachCount =
                response
                        .body()
                        .filter(line -> line.startsWith(suffix))
                        .findFirst()
                        .map(line -> Long.parseLong(line.split(":")[1]))
                        .orElse(0L);

        // Get the configured threshold from the policy
        int threshold =
                Integer.parseInt(
                        context
                                .getRealm()
                                .getPasswordPolicy()
                                .getPolicyConfig(ID));

        if (breachCount >= threshold) {
          return new PolicyError(
                  "pwnedPasswordError",
                  "This password has been exposed in a data breach and cannot be used.");
        }
      }

    } catch (Exception e) {
      return new PolicyError("pwnedPasswordInternalError", "Error while validating password.");
    }

    return null;
  }

  @Override
  public Object parseConfig(String value) {
    return value;
  }

  @Override
  public void close() {
    // No-op
  }
}
