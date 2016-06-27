/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */
package com.forgerock.authenticator.mechanisms.oath;

import com.forgerock.authenticator.mechanisms.URIMappingException;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static junit.framework.Assert.fail;
import static org.testng.Assert.assertEquals;

public class OathAuthMapperTest {

    private OathAuthMapper mapper;

    @Before
    public void setUp() {
        mapper = new OathAuthMapper();
    }

    @Test
    public void shouldParseType() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://hotp/Example:alice@gmail.com?secret=ABC&counter=0");
        assertEquals(result.get(OathAuthMapper.TYPE), "hotp");
    }

    @Test
    public void shouldParseAccountName() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/example?secret=ABC");
        assertEquals(result.get(OathAuthMapper.ACCOUNT_NAME), "example");
    }

    @Test
    public void shouldParseIssuerFromPath() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Badger:ferret?secret=ABC");
        assertEquals(result.get(OathAuthMapper.ISSUER_KEY), "Badger");
    }

    @Test
    public void shouldOverwriteIssuerFromParamters() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Badger:ferret?issuer=Stoat&secret=ABC");
        assertEquals(result.get(OathAuthMapper.ISSUER_KEY), "Stoat");
    }

    @Test
    public void shouldHandleMissingQueryParameters() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Example:alice@google.com?secret=ABC");
        assertEquals(result.get("missing"), null);
    }

    @Test
    public void shouldParseKnownQueryParameters() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP");
        assertEquals(result.get(OathAuthMapper.SECRET), "JBSWY3DPEHPK3PXP");
    }

    @Test
    public void shouldParseUnspecifiedQueryParameters() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&badger=ferret");
        assertEquals(result.get("badger"), "ferret");
    }

    @Test
    public void shouldParseURLEncodedImagePathFromParameter() throws URIMappingException {
        Map<String, String> result = mapper.map("otpauth://totp/Example:alice@google.com?secret=ABC&image=" +
                "http%3A%2F%2Fupload.wikimedia.org%2Fwikipedia%2Fcommons%2F1%2F10%2FBadger-badger.jpg");
        assertEquals(result.get("image"), "http://upload.wikimedia.org/wikipedia/commons/1/10/Badger-badger.jpg");
    }

    @Test (expected = URIMappingException.class)
    public void shouldValidateMissingSecret() throws URIMappingException {
        mapper.map("otpauth://totp/Example:alice@google.com");
    }

    @Test (expected = URIMappingException.class)
    public void shouldValidateMissingCounterInHOTPMode() throws URIMappingException {
        mapper.map("otpauth://hotp/Example:alice@google.com?secret=ABC");
    }

    @Test (expected = URIMappingException.class)
    public void shouldValidateIncorrectAuthorityType() throws URIMappingException {
        mapper.map("otpauth://badger/Example:alice@google.com?secret=ABC");
    }
}