package com.oussema;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.policy.PasswordPolicyProvider;
import org.keycloak.policy.PasswordPolicyProviderFactory;

public class PwnedPasswordPolicyProviderFactory implements PasswordPolicyProviderFactory {

  public static final String ID = "pwnedPassword";
  private static final String DEFAULT_PWNED_THRESHOLD = "1";

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public PasswordPolicyProvider create(KeycloakSession session) {
    return new PwnedPasswordPolicyProvider(session.getContext());
  }

  @Override
  public void init(Config.Scope config) {}

  @Override
  public void postInit(KeycloakSessionFactory factory) {}

  @Override
  public void close() {}

  @Override
  public String getDisplayName() {
    return "Pwned Password";
  }

  @Override
  public String getConfigType() {
    // This allows an integer value to be configured in the UI.
    // It represents the "breach threshold".
    return "int";
  }

  @Override
  public String getDefaultConfigValue() {
    // Reject passwords that have appeared at least once by default.
    return DEFAULT_PWNED_THRESHOLD;
  }

  @Override
  public boolean isMultiplSupported() {
    // This policy should only be added once.
    return false;
  }
}
