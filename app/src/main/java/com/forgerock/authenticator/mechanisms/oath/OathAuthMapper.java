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
 * Copyright 2015-2016 ForgeRock AS.
 */
package com.forgerock.authenticator.mechanisms.oath;

import com.forgerock.authenticator.mechanisms.URIMappingException;
import com.forgerock.authenticator.utils.UriParser;

import java.text.MessageFormat;
import java.util.Map;

/**
 * Provides the ability to parse URI scheme into a convenient format
 * to use with configuring a {@link Oath} to generate OTP codes.
 *
 * The configuration URI is based on the format defined by the original
 * Google Authenticator project:
 *
 * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
 */
class OathAuthMapper extends UriParser {
    /** The secret used for generating the OTP */
    public static final String SECRET = "secret";

    /** The algorithm used for generating the OTP */
    public static final String ALGORITHM = "algorithm";

    /** The number of digits that the OTP should be */
    public static final String DIGITS = "digits";

    /** The counter used to keep track of how many codes have been generated using this mechanism */
    public static final String COUNTER = "counter";

    /** The frequency with which the OTP updates */
    public static final String PERIOD = "period";

    private static final String[] ALLOWED_TYPES = new String[]{"hotp", "totp"};


    @Override
    protected Map<String, String> validate(Map<String, String> values) throws URIMappingException {
        // Validate Type
        String type = values.get(TYPE);
        boolean validType = false;
        for (String allowedType : ALLOWED_TYPES) {
            if (allowedType.equalsIgnoreCase(type)) {
                validType = true;
                break;
            }
        }
        if (!validType) {
            throw new URIMappingException(MessageFormat.format("Type {0} was not valid", type));
        }

        // Secret is REQUIRED
        if (!values.containsKey(SECRET) || values.get(SECRET).isEmpty()) {
            throw new URIMappingException("Secret is required");
        }

        // Counter is REQUIRED
        if (type.equalsIgnoreCase(ALLOWED_TYPES[0])) {
            if (!values.containsKey(COUNTER)) {
                throw new URIMappingException("Counter is required when in hotp mode");
            }
        }

        return values;
    }
}
