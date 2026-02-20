/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 *
 * This code is proprietary and confidential.
 * Unauthorized copying, distribution, or modification is strictly prohibited.
 */
package com.akademiaplus.util.mock.billing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CardPaymentInfoDataGenerator}.
 */
@DisplayName("CardPaymentInfoDataGenerator")
class CardPaymentInfoDataGeneratorTest {

    private CardPaymentInfoDataGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CardPaymentInfoDataGenerator();
    }

    @Nested
    @DisplayName("token()")
    class Token {

        @Test
        @DisplayName("Should generate non-blank token starting with tok_ prefix")
        void shouldGenerateNonBlankToken_startingWithTokPrefix() {
            // Given & When
            String token = generator.token();

            // Then
            assertThat(token).isNotBlank();
            assertThat(token).startsWith("tok_");
        }
    }

    @Nested
    @DisplayName("cardType()")
    class CardType {

        @Test
        @DisplayName("Should generate card type from supported list")
        void shouldGenerateCardType_fromSupportedList() {
            // Given & When
            String cardType = generator.cardType();

            // Then
            assertThat(cardType).isIn(CardPaymentInfoDataGenerator.CARD_TYPES);
        }
    }

    @Nested
    @DisplayName("paymentId()")
    class PaymentId {

        @Test
        @DisplayName("Should generate positive payment ID")
        void shouldGeneratePositivePaymentId() {
            // Given & When
            Long paymentId = generator.paymentId();

            // Then
            assertThat(paymentId).isPositive();
        }
    }
}
