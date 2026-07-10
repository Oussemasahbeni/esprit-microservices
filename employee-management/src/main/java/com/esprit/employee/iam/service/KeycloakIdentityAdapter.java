package com.esprit.employee.iam.service;

import static com.esprit.employee.exception.ErrorCode.*;

import com.esprit.employee.config.keycloak.KeycloakProperties;
import com.esprit.employee.exception.ApplicationException;
import com.esprit.employee.iam.mapper.IamMapper;
import com.esprit.employee.iam.model.IdentityUser;
import com.esprit.employee.iam.model.KeycloakRequiredAction;
import com.esprit.employee.iam.model.Locale;
import com.esprit.employee.iam.model.RoleType;
import com.esprit.employee.iam.utils.KeycloakUtils;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Make sure to enable <b>view-realm</b> and <b>manage-users</b> roles for the client service
 * account in keycloak
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakIdentityAdapter implements IdentityGateway {

  private final RealmResource realmResource;
  private final IamMapper iamMapper;
  private final KeycloakProperties properties;

  @Override
  public Optional<IdentityUser> findById(final String id) {
    try {
      UserRepresentation userRepresentation = realmResource.users().get(id).toRepresentation();
      if (userRepresentation == null) {
        return Optional.empty();
      }
      return Optional.of(mapUser(userRepresentation));
    } catch (NotFoundException _) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<IdentityUser> findByEmail(final String email) {
    List<UserRepresentation> users = realmResource.users().searchByEmail(email, true);
    return getAuthUser(users);
  }

  @Override
  public Optional<IdentityUser> findByUsername(final String username) {
    List<UserRepresentation> users = realmResource.users().searchByUsername(username, true);
    return getAuthUser(users);
  }

  @Override
  public Boolean existsByEmail(final String email) {
    return !realmResource.users().searchByEmail(email, true).isEmpty();
  }

  @Override
  public Page<IdentityUser> findAll(
      final List<String> roles, final String search, final Pageable pageable) {
    List<UserRepresentation> users =
        realmResource.users().search(search, pageable.getPageNumber(), pageable.getPageSize());
    List<IdentityUser> identityUsers = new ArrayList<>();
    users.forEach(user -> identityUsers.add(mapUser(user)));
    sortAuthUsers(identityUsers, pageable.getSort());
    int totalElements = realmResource.users().count();
    return new PageImpl<>(identityUsers, pageable, totalElements);
  }

  @Override
  public List<IdentityUser> findByGroup(final String groupName) {
    List<GroupRepresentation> groups = realmResource.groups().groups();
    GroupRepresentation group =
        groups.stream().filter(g -> g.getName().equals(groupName)).findFirst().orElseThrow();

    List<UserRepresentation> users = realmResource.groups().group(group.getId()).members();
    List<IdentityUser> identityUsers = new ArrayList<>();
    for (UserRepresentation user : users) {
      IdentityUser mappedUser = mapUser(user);
      identityUsers.add(mappedUser);
    }
    return identityUsers;
  }

  @Override
  public List<IdentityUser> findByRole(final RoleType roleType) {
    List<UserRepresentation> users =
        realmResource.roles().get(roleType.getValue()).getUserMembers();
    List<IdentityUser> identityUsers = new ArrayList<>();
    for (UserRepresentation user : users) {
      IdentityUser mappedUser = mapUser(user);
      identityUsers.add(mappedUser);
    }
    return identityUsers;
  }

  @Override
  public List<IdentityUser> findByAttribute(final String key, final String value) {
    var query = String.format("%s:%s", key, value);
    List<UserRepresentation> users = realmResource.users().searchByAttributes(query);
    List<IdentityUser> identityUsers = new ArrayList<>();
    for (UserRepresentation user : users) {
      IdentityUser mappedUser = mapUser(user);
      identityUsers.add(mappedUser);
    }
    return identityUsers;
  }

  @Override
  public IdentityUser create(
      final IdentityUser user,
      final List<KeycloakRequiredAction> actions,
      final boolean sendEmail) {
    try {
      List<RoleType> roleTypes = user.getRoles();

      UserRepresentation userRepresentation = iamMapper.toUserRepresentation(user);
      List<String> requiredActionsAliases = KeycloakRequiredAction.getAliases(actions);
      userRepresentation.setRequiredActions(requiredActionsAliases);
      Response response = realmResource.users().create(userRepresentation);
      String userId = CreatedResponseUtil.getCreatedId(response);
      if (roleTypes != null) {
        roleTypes.forEach(
            role -> {
              RoleRepresentation roleRepresentation =
                  realmResource.roles().get(role.getValue()).toRepresentation();
              realmResource
                  .users()
                  .get(userId)
                  .roles()
                  .realmLevel()
                  .add(Collections.singletonList(roleRepresentation));
            });
      }

      if (sendEmail) {
        //                sendRequiredActionEmail(user, userId, requiredActionsAliases);
      }

      return this.findById(userId)
          .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, "User was not found"));
    } catch (WebApplicationException e) {
      log.error("Error creating user with email: {}", user.getEmail(), e);
      throw new ApplicationException(INVALID_AUTH_REQUEST, "Invalid authentication request");
    }
  }

  @Override
  public IdentityUser update(final IdentityUser user) {
    IdentityUser oldUser =
        this.findById(user.getId())
            .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, "User was not found"));

    IdentityUser newUser = iamMapper.partialUpdate(user, oldUser);
    UserRepresentation userRepresentation = iamMapper.toUserRepresentation(newUser);
    realmResource.users().get(user.getId()).update(userRepresentation);

    return this.findById(user.getId())
        .orElseThrow(() -> new ApplicationException(USER_NOT_FOUND, "User was not found"));
  }

  @Override
  public void deleteById(final String id) {
    try (var _ = realmResource.users().delete(id)) {
      log.info("User with id: {} deleted successfully", id);
    } catch (WebApplicationException e) {
      log.error("Error deleting user with id: {}", id, e);
      throw new ApplicationException(USER_NOT_FOUND, "User was not found");
    }
  }

  @Override
  public IdentityUser enableUser(String userId) {
    UserRepresentation userRepresentation = realmResource.users().get(userId).toRepresentation();
    userRepresentation.setEnabled(true);
    realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
    UserRepresentation userRepresentationUpdated =
        realmResource.users().get(userRepresentation.getId()).toRepresentation();
    return iamMapper.toAuthUser(userRepresentationUpdated);
  }

  @Override
  public IdentityUser disableUser(String userId) {
    UserRepresentation userRepresentation = realmResource.users().get(userId).toRepresentation();
    userRepresentation.setEnabled(false);
    realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
    UserRepresentation userRepresentationUpdated =
        realmResource.users().get(userRepresentation.getId()).toRepresentation();
    return iamMapper.toAuthUser(userRepresentationUpdated);
  }

  @Override
  public void setPassword(final UUID userId, final String password, final boolean temporary) {
    try {
      UserRepresentation userRepresentation =
          realmResource.users().get(String.valueOf(userId)).toRepresentation();
      CredentialRepresentation adminCredentialRepresentation =
          KeycloakUtils.createPasswordCredentials(password, temporary);
      userRepresentation.setCredentials(Collections.singletonList(adminCredentialRepresentation));
      realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
    } catch (WebApplicationException e) {
      log.error("Error setting password for user with id: {}", userId, e);
    }
  }

  @Override
  public void setPassword(
      final UUID userId,
      final String password,
      final Boolean temporary,
      Boolean enable,
      Boolean emailVerified) {
    try {
      UserRepresentation userRepresentation =
          realmResource.users().get(String.valueOf(userId)).toRepresentation();
      CredentialRepresentation adminCredentialRepresentation =
          KeycloakUtils.createPasswordCredentials(password, temporary);
      userRepresentation.setCredentials(Collections.singletonList(adminCredentialRepresentation));
      userRepresentation.setEnabled(enable);
      userRepresentation.setEmailVerified(emailVerified);
      realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
    } catch (WebApplicationException e) {
      log.error("Error setting password for user with id: {}", userId, e);
    }
  }

  @Override
  public IdentityUser updateLocale(String userId, Locale locale) {
    UserRepresentation userRepresentation = realmResource.users().get(userId).toRepresentation();
    userRepresentation.singleAttribute("locale", locale.name());
    realmResource.users().get(userRepresentation.getId()).update(userRepresentation);
    UserRepresentation userRepresentationUpdated =
        realmResource.users().get(userRepresentation.getId()).toRepresentation();
    return iamMapper.toAuthUser(userRepresentationUpdated);
  }

  private void sendRequiredActionEmail(
      IdentityUser identityUser, String userId, List<String> actions) {
    try {
      ClientRepresentation clientRepresentation =
          realmResource.clients().findByClientId(properties.frontendClientId()).getFirst();
      realmResource
          .users()
          .get(userId)
          .executeActionsEmail(
              clientRepresentation.getClientId(), clientRepresentation.getRootUrl(), actions);
    } catch (WebApplicationException e) {
      log.error(
          "Error sending required action email to user with email: {}", identityUser.getEmail(), e);
      throw new ApplicationException(EMAIL_SEND_FAILED, "Error sending required action email");
    }
  }

  private void sortAuthUsers(List<IdentityUser> identityUsers, Sort sort) {
    if (sort.isSorted()) {
      sort.forEach(
          order -> {
            Comparator<IdentityUser> comparator;
            switch (order.getProperty()) {
              case "email" -> comparator = Comparator.comparing(IdentityUser::getEmail);
              case "firstName" -> comparator = Comparator.comparing(IdentityUser::getFirstName);
              case "lastName" -> comparator = Comparator.comparing(IdentityUser::getLastName);
              default -> comparator = Comparator.comparing(IdentityUser::getId);
            }
            if (order.getDirection() == Sort.Direction.DESC) {
              comparator = comparator.reversed();
            }
            identityUsers.sort(comparator);
          });
    }
  }

  private List<RoleType> getUserRoles(String userId) {
    List<RoleRepresentation> realmRoles =
        realmResource.users().get(userId).roles().realmLevel().listAll();

    return realmRoles.stream()
        .map(RoleRepresentation::getName)
        .map(RoleType::fromValue)
        .flatMap(Optional::stream)
        .toList();
  }

  private IdentityUser mapUser(UserRepresentation userRepresentation) {
    List<RoleType> roleTypes = getUserRoles(userRepresentation.getId());
    IdentityUser identityUser = iamMapper.toAuthUser(userRepresentation);

    identityUser.setRoles(roleTypes);
    return identityUser;
  }

  @NotNull
  private Optional<IdentityUser> getAuthUser(List<UserRepresentation> users) {
    if (!users.isEmpty()) {
      var user = users.getFirst();
      return Optional.of(this.mapUser(user));
    } else {
      return Optional.empty();
    }
  }
}
