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
 * Copyright 2016 ForgeRock AS.
 */

package com.forgerock.authenticator.utils;

import com.forgerock.authenticator.mechanisms.URIMappingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for converting mechanism URIs to useful data. Extracts common information (scheme, type,
 * version, issuer and account name). All information stored as parameters are converted to a map.
 * Subclasses on this class must implement validate(), which verifies that all required information
 * is present.
 */
public abstract class UriParser {
    /** The protocol of the URI */
    public static final String SCHEME = "scheme";

    /** The type of OTP (TOTP or HOTP) */
    public static final String TYPE = "authority"; // URI refers to this as the authority.
    /** The URI API Version */
    public static final String VERSION = "version";

    /** The IDP that issued the URI */
    public static final String ISSUER = "issuer";

    /** The identity account name */
    public static final String ACCOUNT_NAME = "accountname";

    private static final String SLASH = "/";

    /**
     * Call through to {@link UriParser#map(URI)}
     *
     * @param uriScheme Non null.
     * @return Non null, possibly empty Map.
     * @throws URIMappingException If there was an unexpected error parsing.
     */
    public Map<String, String> map(String uriScheme) throws URIMappingException {
        try {
            return validate(map(new URI(uriScheme)));
        } catch (URISyntaxException e) {
            throw new URIMappingException("Failed to parse URI", e);
        }
    }


    /**
     * Parse the URI into a more useful Map format with known keys.
     *
     * Makes use of the Java provided {@link URI} to simplify parsing.
     *
     * @param uri Non null URI to parse.
     * @return Non null possibly empty Map.
     * @throws URIMappingException If there was an unexpected error parsing.
     */
    private Map<String, String> map(URI uri) throws URIMappingException {
        Map<String, String> r = new HashMap<String, String>();
        r.put(SCHEME, uri.getScheme());
        r.put(TYPE, uri.getAuthority());

        // Label may contain Issuer and Account Name
        String path = stripSlash(uri.getPath());
        String[] pathParts = split(path, ":");
        if (pathParts == null) {
            r.put(ACCOUNT_NAME, path);
        } else {
            r.put(ISSUER, pathParts[0]);
            r.put(ACCOUNT_NAME, pathParts[1]);
        }

        Collection<String> queryParts = Collections.emptySet();
        if (uri.getQuery() != null) {
            queryParts = Arrays.asList(uri.getQuery().split("&"));
        }
        for (String query : queryParts) {
            String[] split = split(query, "=");
            if (split != null) {
                r.put(split[0], split[1]);
            }
        }
        return r;
    }

    /**
     * Validates the parsed URI values based on the requirements of the current
     * Google Authenticator specification.
     *
     * @param values The non null map of values stored in the parameters in the URI.
     * @return The same map of values, with a transform applied if required.
     * @throws URIMappingException If there were any validation errors.
     */
    protected abstract Map<String, String> validate(Map<String, String> values) throws URIMappingException;

    private static String[] split(String s, String sep) {
        int index = s.indexOf(sep);
        if (index == -1) {
            return null;
        }
        return new String[]{
                s.substring(0, index),
                s.substring(index + sep.length(), s.length())};
    }

    private static String stripSlash(String s) {
        if (s.startsWith(SLASH)) {
            return stripSlash(s.substring(SLASH.length(), s.length()));
        }
        if (s.endsWith(SLASH)) {
            return stripSlash(s.substring(0, s.length() - SLASH.length()));
        }
        return s;
    }
}
