/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import com.akademiaplus.internal.interfaceadapters.filters.AppOriginContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("SecurityConfig")
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Nested
    @DisplayName("ModuleSecurityConfigurator")
    class ModuleSecurityConfiguratorTests {

        @Mock
        private AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth;

        @Test
        @DisplayName("Should call configure with akademia origin when app origin aware method invoked")
        void shouldCallConfigureWithAkademiaOrigin_whenAppOriginAwareMethodInvoked() throws Exception {
            // Given
            ModuleSecurityConfigurator configurator = mock(ModuleSecurityConfigurator.class);
            doCallRealMethod().when(configurator).configure(auth, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            configurator.configure(auth, AppOriginContext.ORIGIN_AKADEMIA);

            // Then
            verify(configurator).configure(auth, AppOriginContext.ORIGIN_AKADEMIA);
            verify(configurator).configure(auth);
        }

        @Test
        @DisplayName("Should call configure with elatus origin when app origin aware method invoked")
        void shouldCallConfigureWithElatusOrigin_whenAppOriginAwareMethodInvoked() throws Exception {
            // Given
            ModuleSecurityConfigurator configurator = mock(ModuleSecurityConfigurator.class);
            doCallRealMethod().when(configurator).configure(auth, AppOriginContext.ORIGIN_ELATUS);

            // When
            configurator.configure(auth, AppOriginContext.ORIGIN_ELATUS);

            // Then
            verify(configurator).configure(auth, AppOriginContext.ORIGIN_ELATUS);
            verify(configurator).configure(auth);
        }

        @Test
        @DisplayName("Should delegate to base configure when default method used")
        void shouldDelegateToBaseConfigure_whenDefaultMethodUsed() throws Exception {
            // Given
            ArgumentCaptor<String> originCaptor = ArgumentCaptor.forClass(String.class);
            ModuleSecurityConfigurator configurator = mock(ModuleSecurityConfigurator.class);
            doCallRealMethod().when(configurator).configure(auth, AppOriginContext.ORIGIN_AKADEMIA);

            // When
            configurator.configure(auth, AppOriginContext.ORIGIN_AKADEMIA);

            // Then
            verify(configurator).configure(auth);
            verifyNoMoreInteractions(auth);
        }
    }

    @Nested
    @DisplayName("AppSecurityProperties")
    class AppSecurityPropertiesTests {

        @Test
        @DisplayName("Should return default values when no properties set")
        void shouldReturnDefaultValues_whenNoPropertiesSet() {
            // Given
            AppSecurityProperties properties = new AppSecurityProperties();

            // When & Then
            assertThat(properties.getAkademia()).isNotNull();
            assertThat(properties.getElatus()).isNotNull();
            assertThat(properties.getAkademia().isTokenBindingEnabled()).isFalse();
            assertThat(properties.getAkademia().isRateLimitingEnabled()).isFalse();
            assertThat(properties.getAkademia().isHmacVerificationEnabled()).isFalse();
            assertThat(properties.getAkademia().getAllowedOrigins()).isEmpty();
        }

        @Test
        @DisplayName("Should store configured values when properties set")
        void shouldStoreConfiguredValues_whenPropertiesSet() {
            // Given
            AppSecurityProperties properties = new AppSecurityProperties();
            AppSecurityProperties.AppConfig elatusConfig = new AppSecurityProperties.AppConfig();
            elatusConfig.setTokenBindingEnabled(true);
            elatusConfig.setRateLimitingEnabled(true);
            elatusConfig.setHmacVerificationEnabled(true);
            String[] origins = {"http://localhost:3001"};
            elatusConfig.setAllowedOrigins(origins);

            // When
            properties.setElatus(elatusConfig);

            // Then
            assertThat(properties.getElatus().isTokenBindingEnabled()).isTrue();
            assertThat(properties.getElatus().isRateLimitingEnabled()).isTrue();
            assertThat(properties.getElatus().isHmacVerificationEnabled()).isTrue();
            assertThat(properties.getElatus().getAllowedOrigins()).containsExactly("http://localhost:3001");
        }
    }
}
