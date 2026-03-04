/*
 * Copyright (c) 2025 ElatusDev
 * All rights reserved.
 */
package com.akademiaplus.usecases;

import com.akademiaplus.usecases.domain.JwksRegistry;
import com.akademiaplus.usecases.domain.JwksRegistry.JwksEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import openapi.akademiaplus.domain.certificate.authority.dto.JwkKeyDTO;
import openapi.akademiaplus.domain.certificate.authority.dto.JwksDocumentDTO;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Builds a standards-compliant JWKS document (RFC 7517) from all registered
 * service public keys stored in {@link JwksRegistry}.
 *
 * <p>Supports EC (P-256, P-384, P-521) and RSA public keys. Unknown key types
 * are logged and skipped rather than failing the entire endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetJwksUseCase {

    private final JwksRegistry jwksRegistry;

    public JwksDocumentDTO getJwks() {
        List<JwkKeyDTO> keys = new ArrayList<>();

        for (JwksEntry entry : jwksRegistry.getEntries()) {
            try {
                keys.add(toJwk(entry));
            } catch (java.security.GeneralSecurityException e) {
                log.warn("Skipping JWKS entry kid={} — could not parse public key: {}", entry.getKid(), e.getMessage());
            }
        }

        JwksDocumentDTO doc = new JwksDocumentDTO();
        doc.setKeys(keys);
        return doc;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private JwkKeyDTO toJwk(JwksEntry entry) throws java.security.GeneralSecurityException {
        byte[] derBytes = jwksRegistry.decodePublicKey(entry);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(derBytes);

        // Try EC first, then RSA
        try {
            ECPublicKey ecKey = (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(spec);
            return buildEcJwk(entry.getKid(), entry.getAlg(), ecKey);
        } catch (java.security.GeneralSecurityException _) {
            // not EC — try RSA below
        }
        RSAPublicKey rsaKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        return buildRsaJwk(entry.getKid(), entry.getAlg(), rsaKey);
    }

    private JwkKeyDTO buildEcJwk(String kid, String alg, ECPublicKey key) {
        JwkKeyDTO jwk = new JwkKeyDTO();
        jwk.setKty("EC");
        jwk.setUse("sig");
        jwk.setKid(kid);
        jwk.setAlg(alg);

        // Derive curve name from key size
        int bitLength = key.getParams().getOrder().bitLength();
        String crv = switch (bitLength) {
            case 256 -> "P-256";
            case 384 -> "P-384";
            case 521 -> "P-521";
            default  -> "P-" + bitLength;
        };
        jwk.setCrv(crv);

        // Extract affine coordinates, zero-padded to the field size in bytes
        int coordLen = (bitLength + 7) / 8;
        jwk.setX(base64UrlEncode(padLeft(key.getW().getAffineX().toByteArray(), coordLen)));
        jwk.setY(base64UrlEncode(padLeft(key.getW().getAffineY().toByteArray(), coordLen)));
        return jwk;
    }

    private JwkKeyDTO buildRsaJwk(String kid, String alg, RSAPublicKey key) {
        JwkKeyDTO jwk = new JwkKeyDTO();
        jwk.setKty("RSA");
        jwk.setUse("sig");
        jwk.setKid(kid);
        jwk.setAlg(alg);
        jwk.setN(base64UrlEncode(unsignedBytes(key.getModulus())));
        jwk.setE(base64UrlEncode(unsignedBytes(key.getPublicExponent())));
        return jwk;
    }

    /** Strip leading zero byte (sign bit) that BigInteger may add. */
    private byte[] unsignedBytes(BigInteger value) {
        byte[] bytes = value.toByteArray();
        if (bytes[0] == 0 && bytes.length > 1) {
            byte[] trimmed = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return bytes;
    }

    /** Pad or trim to exact length (EC coordinates need fixed-width). */
    private byte[] padLeft(byte[] src, int length) {
        if (src.length == length) return src;
        if (src.length > length) {
            // BigInteger may have a leading zero sign byte
            byte[] trimmed = new byte[length];
            System.arraycopy(src, src.length - length, trimmed, 0, length);
            return trimmed;
        }
        byte[] padded = new byte[length];
        System.arraycopy(src, 0, padded, length - src.length, src.length);
        return padded;
    }

    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
