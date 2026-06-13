package com.esprit.employee.iam.mapper;


import com.esprit.employee.iam.model.IdentityUser;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.*;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface IamMapper {

    IdentityUser partialUpdate(IdentityUser user, @MappingTarget IdentityUser identityUser);

    UserRepresentation toUserRepresentation(IdentityUser user);

    IdentityUser toAuthUser(UserRepresentation userRepresentation);

}
