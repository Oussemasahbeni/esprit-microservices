package com.esprit.employee.iam.model;

import lombok.Getter;

import java.util.List;

@Getter
public enum KeycloakRequiredAction {
    // Standard Required Actions
    CONFIGURE_TOTP("CONFIGURE_TOTP"),
    TERMS_AND_CONDITIONS("TERMS_AND_CONDITIONS"),
    UPDATE_PASSWORD("UPDATE_PASSWORD"),
    UPDATE_PROFILE("UPDATE_PROFILE"),
    VERIFY_EMAIL("VERIFY_EMAIL"),
    UPDATE_EMAIL("UPDATE_EMAIL"),
    VERIFY_PROFILE("VERIFY_PROFILE"),
    IDP_LINK("idp_link"),
    CONFIGURE_RECOVERY_AUTHN_CODES("CONFIGURE_RECOVERY_AUTHN_CODES"),

    // WebAuthn/Credential related
    WEBAUTHN_REGISTER("webauthn-register"),
    WEBAUTHN_REGISTER_PASSWORDLESS("webauthn-register-passwordless"),
    DELETE_CREDENTIAL("delete_credential"),

    // Account Management
    DELETE_ACCOUNT("delete_account"),

    UPDATE_USER_LOCALE("update_user_locale");

    private final String alias;

    KeycloakRequiredAction(String alias) {
        this.alias = alias;
    }

    public static List<String> getAliases(List<KeycloakRequiredAction> actions) {
        return actions.stream().map(KeycloakRequiredAction::getAlias).toList();
    }
}
