package com.esprit.employee.iam.utils;

import org.keycloak.representations.idm.CredentialRepresentation;

public class KeycloakUtils {

    private KeycloakUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static CredentialRepresentation createPasswordCredentials(String password, boolean temporary) {
        CredentialRepresentation passwordCredentials = new CredentialRepresentation();
        passwordCredentials.setTemporary(temporary);
        passwordCredentials.setType(CredentialRepresentation.PASSWORD);
        passwordCredentials.setValue(password);
        return passwordCredentials;
    }
}
