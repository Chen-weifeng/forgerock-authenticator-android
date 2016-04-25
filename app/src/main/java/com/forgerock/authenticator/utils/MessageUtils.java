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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Utility methods related to Messages received from OpenAM.
 */
public class MessageUtils {

    /**
     * Used to respond to a given message ID at a given endpoint.
     * @param endpoint The endpoint to respond to.
     * @param messageId The id of the message being responded to.
     * @param data The data to attach to the response.
     * @return The response code of the request.
     * @throws IOException If a network issue occurred.
     * @throws JSONException If an encoding issue occurred.
     */
    public static int respond(String endpoint, String messageId, Map<String, String> data)
            throws IOException, JSONException {
        HttpURLConnection connection = null;
        int returnCode = 404;
        try {
            URL url = new URL(endpoint);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();

            JSONObject jsonData = new JSONObject();
            for (String key : data.keySet()) {
                jsonData.put(key, data.get(key));
            }
            jsonData.put("messageId", messageId);
            JSONObject message = new JSONObject();
            message.put("data", jsonData);

            OutputStream os = connection.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
            osw.write(message.toString());
            osw.flush();
            osw.close();
            returnCode = connection.getResponseCode();
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
        return returnCode;
    }
}
