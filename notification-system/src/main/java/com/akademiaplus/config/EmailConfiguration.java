/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Configures the {@link JavaMailSender} bean for SMTP-based email delivery.
 *
 * <p>Supports any SMTP-compatible provider (AWS SES, Mailtrap, etc.)
 * via externalized properties.
 *
 * @author ElatusDev
 * @since 1.0
 */
@Configuration
public class EmailConfiguration {

    /** Default SMTP port for STARTTLS connections. */
    public static final int DEFAULT_SMTP_PORT = 587;

    /** JavaMail property key for transport protocol. */
    public static final String PROP_TRANSPORT_PROTOCOL = "mail.transport.protocol";

    /** JavaMail property key for SMTP authentication. */
    public static final String PROP_SMTP_AUTH = "mail.smtp.auth";

    /** JavaMail property key for STARTTLS enablement. */
    public static final String PROP_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    /** JavaMail property key for STARTTLS requirement. */
    public static final String PROP_SMTP_STARTTLS_REQUIRED = "mail.smtp.starttls.required";

    /**
     * Creates a {@link JavaMailSender} configured with SMTP properties.
     *
     * @param host     SMTP server hostname
     * @param port     SMTP server port
     * @param username SMTP authentication username
     * @param password SMTP authentication password
     * @return configured JavaMailSender
     */
    @Bean
    public JavaMailSender emailMailSender(
            @Value("${akademia.email.host}") String host,
            @Value("${akademia.email.port}") int port,
            @Value("${akademia.email.username}") String username,
            @Value("${akademia.email.password}") String password) {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put(PROP_TRANSPORT_PROTOCOL, "smtp");
        props.put(PROP_SMTP_AUTH, "true");
        props.put(PROP_SMTP_STARTTLS_ENABLE, "true");
        props.put(PROP_SMTP_STARTTLS_REQUIRED, "true");

        return mailSender;
    }
}
