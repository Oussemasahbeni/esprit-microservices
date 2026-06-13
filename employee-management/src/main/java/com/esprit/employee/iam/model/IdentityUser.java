package com.esprit.employee.iam.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class IdentityUser {

    private String id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private Boolean enabled;
    private Boolean emailVerified;
    private Map<String, List<String>> attributes;
    private List<RoleType> roles;
}