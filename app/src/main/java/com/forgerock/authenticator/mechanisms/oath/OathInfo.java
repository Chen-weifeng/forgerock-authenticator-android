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

package com.forgerock.authenticator.mechanisms.oath;

import android.view.View;

import com.forgerock.authenticator.R;
import com.forgerock.authenticator.mechanisms.base.Mechanism;
import com.forgerock.authenticator.mechanisms.base.MechanismFactory;
import com.forgerock.authenticator.mechanisms.base.MechanismInfo;


/**
 * Provides information about the OTP mechanisms.
 * Collects together all the information required to create, store and wire the OTP mechanism into
 * the user interface.
 */
public class OathInfo implements MechanismInfo {
    @Override
    public void bind(View view, Mechanism mechanism) {
        OathLayout oathLayout = (OathLayout) view;
        oathLayout.bind((Oath) mechanism);
    }

    @Override
    public int getLayoutType() {
        return R.layout.tokencell;
    }

    @Override
    public int getIcon() {
        return R.drawable.forgerock_icon_oath;
    }

    @Override
    public String getMechanismString() {
        return "OTP";
    }

    @Override
    public MechanismFactory getFactory() {
        return new OathFactory();
    }

    @Override
    public boolean matchesURI(String uri) {
        return uri.startsWith("otpauth://");
    }
}
