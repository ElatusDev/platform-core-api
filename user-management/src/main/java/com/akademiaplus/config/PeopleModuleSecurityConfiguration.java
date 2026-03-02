/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.utilities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@PropertySource("classpath:people.properties")
@Configuration
public class PeopleModuleSecurityConfiguration implements ModuleSecurityConfigurator {

    private final String employeePath;
    private final String employeePathAnyPathVars;
    private final String collaboratorPath;
    private final String collaboratorPathAnyPathVars;
    private final String adultStudentPath;
    private final String adultStudentPathAnyPathVar;
    private final String tutorPath;
    private final String tutorPathAnyPathVars;
    private final String minorStudentPath;
    private final String minorStudentPathAnyPathVars;

    public PeopleModuleSecurityConfiguration(@Value("${api.user-management.employee.base-url}") String employeeBaseUri,
                                             @Value("${api.user-management.collaborator.base-url}") String collaboratorBaseUri,
                                             @Value("${api.user-management.adult-student.base-url}") String adultStudentBaseUri,
                                             @Value("${api.user-management.tutor.base-url}") String tutorBaseUri,
                                             @Value("${api.user-management.minor-student.base-url}") String minorStudentBaseUri) {
        String anyPathVar = "/**";
        employeePath = employeeBaseUri;
        employeePathAnyPathVars = employeePath + anyPathVar;
        collaboratorPath = collaboratorBaseUri;
        collaboratorPathAnyPathVars = collaboratorPath + anyPathVar;
        adultStudentPath = adultStudentBaseUri;
        adultStudentPathAnyPathVar = adultStudentPath + anyPathVar;
        tutorPath = tutorBaseUri;
        tutorPathAnyPathVars = tutorPath + anyPathVar;
        minorStudentPath = minorStudentBaseUri;
        minorStudentPathAnyPathVars = minorStudentPath + anyPathVar;
    }

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
            employeePaths(auth);
            collaboratorPaths(auth);
            adultStudentPaths(auth);
            tutorPaths(auth);
            minorStudentPaths(auth);
    }

    public void employeePaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, employeePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, employeePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, employeePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, employeePathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, employeePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, employeePath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    public void collaboratorPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, collaboratorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, collaboratorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, collaboratorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, collaboratorPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, collaboratorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, collaboratorPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    public void adultStudentPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, adultStudentPathAnyPathVar)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, adultStudentPathAnyPathVar)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, adultStudentPathAnyPathVar)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, adultStudentPathAnyPathVar)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, adultStudentPathAnyPathVar)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, adultStudentPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    public void tutorPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, tutorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, tutorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, tutorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, tutorPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, tutorPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, tutorPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    public void minorStudentPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, minorStudentPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, minorStudentPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, minorStudentPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, minorStudentPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, minorStudentPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, minorStudentPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

}