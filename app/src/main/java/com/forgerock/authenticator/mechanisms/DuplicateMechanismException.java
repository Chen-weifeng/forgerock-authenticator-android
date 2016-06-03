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
package com.forgerock.authenticator.mechanisms;

import com.forgerock.authenticator.mechanisms.base.Mechanism;

/**
 * Represents an error in setting up a mechanism, caused by a matching mechanism already existing.
 */
public class DuplicateMechanismException extends MechanismCreationException {
    private Mechanism cause;

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     * @param throwable The throwable cause of the exception.
     */
    public DuplicateMechanismException(String detailMessage, Mechanism cause, Throwable throwable) {
        super(detailMessage, throwable);
        this.cause = cause;
    }

    /**
     * Create a new exception containing a message.
     * @param detailMessage The message cause of the exception.
     */
    public DuplicateMechanismException(String detailMessage, Mechanism cause) {
        super(detailMessage);
        this.cause = cause;
    }

    public Mechanism getCausingMechanism() {
        return cause;
    }
}
