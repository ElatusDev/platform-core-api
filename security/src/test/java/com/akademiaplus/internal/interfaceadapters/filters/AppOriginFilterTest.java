/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.internal.interfaceadapters.filters;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AppOriginFilter")
class AppOriginFilterTest {

    private AppOriginFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new AppOriginFilter();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Nested
    @DisplayName("Header Resolution")
    class HeaderResolution {

        @Test
        @DisplayName("Should set akademia origin when header is akademia")
        void shouldSetAkademiaOrigin_whenHeaderIsAkademia() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_AKADEMIA);
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should set elatus origin when header is elatus")
        void shouldSetElatusOrigin_whenHeaderIsElatus() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_ELATUS);
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }

        @Test
        @DisplayName("Should ignore invalid header when header value is unknown")
        void shouldIgnoreInvalidHeader_whenHeaderValueIsUnknown() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, "unknown-app");
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Path Resolution")
    class PathResolution {

        @Test
        @DisplayName("Should set akademia origin when path starts with akademia")
        void shouldSetAkademiaOrigin_whenPathStartsWithAkademia() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/akademia/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_AKADEMIA);
        }

        @Test
        @DisplayName("Should set elatus origin when path starts with elatus")
        void shouldSetElatusOrigin_whenPathStartsWithElatus() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/elatus/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }
    }

    @Nested
    @DisplayName("Default Resolution")
    class DefaultResolution {

        @Test
        @DisplayName("Should default to elatus when no header or path match")
        void shouldDefaultToElatus_whenNoHeaderOrPathMatch() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.ORIGIN_ELATUS);
        }

        @Test
        @DisplayName("Should default to elatus when header is invalid and no path match")
        void shouldDefaultToElatus_whenHeaderIsInvalidAndNoPathMatch() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, "invalid-value");
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(AppOriginContext.getAppOrigin(request)).isEqualTo(AppOriginContext.DEFAULT_ORIGIN);
        }
    }

    @Nested
    @DisplayName("Filter Chain")
    class FilterChainTests {

        @Test
        @DisplayName("Should continue filter chain when origin resolved")
        void shouldContinueFilterChain_whenOriginResolved() throws ServletException, IOException {
            // Given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(AppOriginContext.APP_ORIGIN_HEADER, AppOriginContext.ORIGIN_AKADEMIA);
            request.setRequestURI("/v1/some/endpoint");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            assertThat(filterChain.getRequest()).isNotNull();
        }
    }
}
