/*
 * Copyright (c) 2026 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.magiclink.usecases;

import com.akademiaplus.config.MagicLinkProperties;
import com.akademiaplus.infra.persistence.config.TenantContextHolder;
import com.akademiaplus.magiclink.interfaceadapters.MagicLinkTokenRepository;
import com.akademiaplus.ratelimit.usecases.RateLimiterService;
import com.akademiaplus.ratelimit.usecases.domain.RateLimitResult;
import com.akademiaplus.security.MagicLinkTokenDataModel;
import com.akademiaplus.utilities.security.HashingService;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.security.dto.MagicLinkRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Generates and emails magic link tokens for passwordless authentication.
 *
 * <p>Generates a 32-byte cryptographically random token, stores its
 * SHA-256 hash in the database, and sends the raw token to the user's
 * email. Always returns silently (anti-enumeration).</p>
 *
 * @author ElatusDev
 * @since 1.0
 */
@Slf4j
@Service
public class MagicLinkRequestUseCase {

    /** Redis key prefix for per-email rate limiting. */
    public static final String RATE_LIMIT_KEY_PREFIX = "rate:magic-link:email:";

    /** Number of random bytes for token generation (256-bit entropy). */
    public static final int TOKEN_BYTE_LENGTH = 32;

    /** URL template for the magic link. */
    public static final String MAGIC_LINK_URL_TEMPLATE = "%s/auth/magic-link?token=%s&tenant=%d";

    /** Rate limit window in milliseconds (1 hour). */
    public static final long RATE_LIMIT_WINDOW_MS = 3_600_000L;

    /** HTML email template with placeholders for the magic link URL. */
    public static final String EMAIL_TEMPLATE = """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"></head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
              <h2>Sign in to ElatusDev</h2>
              <p>Click the button below to sign in. This link expires in 10 minutes.</p>
              <a href="%s"
                 style="display: inline-block; padding: 12px 24px; background-color: #4F46E5;\
             color: white; text-decoration: none; border-radius: 6px; margin: 16px 0;">
                Sign In
              </a>
              <p style="color: #666; font-size: 14px;">
                If you didn't request this link, you can safely ignore this email.
              </p>
              <p style="color: #999; font-size: 12px;">
                Or copy this link: %s
              </p>
            </body>
            </html>
            """;

    private final MagicLinkTokenRepository magicLinkTokenRepository;
    private final HashingService hashingService;
    private final MagicLinkProperties properties;
    private final TenantContextHolder tenantContextHolder;
    private final ApplicationContext applicationContext;
    private final MagicLinkEmailSender emailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired(required = false)
    private RateLimiterService rateLimiterService;

    /**
     * Constructs the use case with required dependencies.
     *
     * @param magicLinkTokenRepository the token repository
     * @param hashingService           the SHA-256 hashing service
     * @param properties               the magic link configuration
     * @param tenantContextHolder      the tenant context holder
     * @param applicationContext       the Spring application context for bean creation
     * @param emailSender              the magic link email sender
     */
    public MagicLinkRequestUseCase(MagicLinkTokenRepository magicLinkTokenRepository,
                                   HashingService hashingService,
                                   MagicLinkProperties properties,
                                   TenantContextHolder tenantContextHolder,
                                   ApplicationContext applicationContext,
                                   MagicLinkEmailSender emailSender) {
        this.magicLinkTokenRepository = magicLinkTokenRepository;
        this.hashingService = hashingService;
        this.properties = properties;
        this.tenantContextHolder = tenantContextHolder;
        this.applicationContext = applicationContext;
        this.emailSender = emailSender;
    }

    /**
     * Processes a magic link request by generating a token and sending an email.
     *
     * <p>Always completes silently — never throws an exception, even if the
     * email does not exist or rate limiting is exceeded. This prevents email
     * enumeration attacks.</p>
     *
     * @param dto the magic link request containing email and tenant ID
     */
    public void requestMagicLink(MagicLinkRequestDTO dto) {
        tenantContextHolder.setTenantId(dto.getTenantId());

        if (isRateLimited(dto.getEmail())) {
            return;
        }

        String rawToken = generateToken();
        String tokenHash = hashingService.generateHash(rawToken);

        storeToken(dto, tokenHash);

        String magicLinkUrl = buildMagicLinkUrl(rawToken, dto.getTenantId());
        String htmlContent = String.format(EMAIL_TEMPLATE, magicLinkUrl, magicLinkUrl);
        emailSender.send(dto.getEmail(), properties.emailSubject(), htmlContent);
    }

    private boolean isRateLimited(String email) {
        if (rateLimiterService == null) {
            return false;
        }
        RateLimitResult result = rateLimiterService.checkRateLimit(
                RATE_LIMIT_KEY_PREFIX + email,
                properties.maxRequestsPerEmailPerHour(),
                RATE_LIMIT_WINDOW_MS);
        return !result.allowed();
    }

    private String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private void storeToken(MagicLinkRequestDTO dto, String tokenHash) {
        MagicLinkTokenDataModel token = applicationContext.getBean(MagicLinkTokenDataModel.class);
        token.setTenantId(dto.getTenantId());
        token.setEmail(dto.getEmail());
        token.setTokenHash(tokenHash);
        token.setExpiresAt(Instant.now().plus(properties.tokenExpiryMinutes(), ChronoUnit.MINUTES));
        magicLinkTokenRepository.save(token);
    }

    private String buildMagicLinkUrl(String rawToken, Long tenantId) {
        return String.format(MAGIC_LINK_URL_TEMPLATE, properties.baseUrl(), rawToken, tenantId);
    }
}
