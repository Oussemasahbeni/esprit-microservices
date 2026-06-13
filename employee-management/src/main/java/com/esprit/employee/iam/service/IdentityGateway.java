package com.esprit.employee.iam.service;


import com.esprit.employee.iam.model.IdentityUser;
import com.esprit.employee.iam.model.KeycloakRequiredAction;
import com.esprit.employee.iam.model.Locale;
import com.esprit.employee.iam.model.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityGateway {

    /**
     * Finds a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<IdentityUser> findById(String id);

    /**
     * Finds a user by their email address.
     *
     * @param email the email address of the user
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<IdentityUser> findByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username the username of the user
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<IdentityUser> findByUsername(String username);

    Boolean existsByEmail(String email);

    /**
     * Retrieves all users with pagination and filtering options.
     *
     * @param roles    the list of roles to filter by
     * @param search   the search term for filtering users
     * @param pageable the pagination information
     * @return a page of users matching the criteria
     */
    Page<IdentityUser> findAll(List<String> roles, String search, Pageable pageable);

    /**
     * Finds all users belonging to a specific group.
     *
     * @param groupId the unique identifier of the group
     * @return a list of users in the specified group
     */
    List<IdentityUser> findByGroup(String groupId);

    /**
     * Finds all users with a specific role type.
     *
     * @param roleType the role type to filter by
     * @return a list of users with the specified role type
     */
    List<IdentityUser> findByRole(RoleType roleType);

    /**
     * Finds users based on a specified attribute key and its corresponding value.
     *
     * @param key   the attribute key to search for (e.g., "department", "location")
     * @param value the specific value of the attribute to match
     * @return a list of {@code IdentityUser} objects that have the specified key-value attribute pair
     */
    List<IdentityUser> findByAttribute(String key, String value);

    /**
     * Creates a new user in the identity provider.
     *
     * @param user            the user to create
     * @param requiredActions the list of required actions for the user
     * @param sendEmail       whether to send a welcome email
     * @return the created user
     */
    IdentityUser create(IdentityUser user, List<KeycloakRequiredAction> requiredActions, boolean sendEmail);

    /**
     * Updates an existing user in the identity provider.
     *
     * @param user the user with updated information
     * @return the updated user
     */
    IdentityUser update(IdentityUser user);

    /**
     * Deletes a user by their unique identifier.
     *
     * @param id the unique identifier of the user to delete
     */
    void deleteById(String id);

    /**
     * Enables a user account.
     *
     * @param id the unique identifier of the user to enable
     * @return the enabled user
     */
    IdentityUser enableUser(String id);

    /**
     * Disables a user account.
     *
     * @param id the unique identifier of the user to disable
     * @return the disabled user
     */
    IdentityUser disableUser(String id);

    /**
     * Sets the password for a user.
     *
     * @param userId    the unique identifier of the user for whom the password is being set
     * @param password  the new password to be assigned to the user
     * @param temporary a flag indicating whether the password is temporary; if true, the user may be
     *                  prompted to change it upon next login
     */
    void setPassword(UUID userId, String password, boolean temporary);

    /**
     * Sets the password for a user with additional configuration options.
     *
     * @param userId        the unique identifier of the user for whom the password is being set
     * @param password      the new password to be assigned to the user
     * @param temporary     a flag indicating whether the password is temporary; if true, the user may be
     *                      prompted to change it upon next login
     * @param enable        a flag indicating whether the user's account should be enabled after setting the
     *                      password
     * @param emailVerified a flag indicating whether the user's email address should be marked as
     *                      verified
     */
    void setPassword(
            UUID userId, String password, Boolean temporary, Boolean enable, Boolean emailVerified);

    /**
     * Updates the locale preference for a user.
     *
     * @param userId the unique identifier of the user
     * @param locale the new locale preference
     * @return the user with updated locale
     */
    IdentityUser updateLocale(String userId, Locale locale);

}
