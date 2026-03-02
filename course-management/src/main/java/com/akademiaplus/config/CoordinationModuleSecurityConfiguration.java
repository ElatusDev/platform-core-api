package com.akademiaplus.config;


import com.akademiaplus.utilities.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@PropertySource("classpath:coordination.properties")
@Configuration
public class CoordinationModuleSecurityConfiguration implements ModuleSecurityConfigurator {

    private final String coursePath;
    private final String coursePathAnyPathVars;
    private final String schedulePath;
    private final String schedulePathAnyPathVars;
    private final String courseEventPath;
    private final String courseEventPathAnyPathVars;

    public CoordinationModuleSecurityConfiguration(@Value("${api.coordination.course.base-url}") String coursePath,
                                                   @Value("${api.coordination.schedule.base-url}") String schedulePath,
                                                   @Value("${api.coordination.course-event.base-url}") String courseEventPath) {
        String anyPathVar = "/**";
        this.coursePath = coursePath;
        this.coursePathAnyPathVars = coursePath + anyPathVar;
        this.schedulePath = schedulePath;
        this.schedulePathAnyPathVars = schedulePath + anyPathVar;
        this.courseEventPath = courseEventPath;
        this.courseEventPathAnyPathVars = courseEventPath + anyPathVar;
    }

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) throws Exception {
        coursePaths(auth);
        schedulePaths(auth);
        courseEventPaths(auth);
    }

    private void coursePaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, coursePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, coursePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, coursePath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, coursePathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, coursePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, coursePath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    private void schedulePaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, schedulePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, schedulePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, schedulePath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, schedulePathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, schedulePathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, schedulePath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }

    private void courseEventPaths(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                .requestMatchers(HttpMethod.DELETE, courseEventPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name())
                .requestMatchers(HttpMethod.GET, courseEventPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.POST, courseEventPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.PUT, courseEventPathAnyPathVars)
                .denyAll()
                .requestMatchers(HttpMethod.PATCH, courseEventPathAnyPathVars)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name())
                .requestMatchers(HttpMethod.GET, courseEventPath)
                .hasAnyRole(Role.ADMIN.name(), Role.PRINCIPAL.name(), Role.CSR.name());
    }
}
