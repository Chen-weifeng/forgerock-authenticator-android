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

package com.forgerock.authenticator.identity;

import android.content.Context;
import android.net.Uri;

import com.forgerock.authenticator.mechanisms.MechanismCreationException;
import com.forgerock.authenticator.mechanisms.base.Mechanism;
import com.forgerock.authenticator.model.ModelObject;
import com.forgerock.authenticator.model.SortedList;
import com.forgerock.authenticator.storage.IdentityDatabase;
import com.forgerock.authenticator.storage.IdentityModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import roboguice.RoboGuice;

/**
 * Identity is responsible for modelling the information that makes up part of a users identity in
 * the context of logging into that users account.
 */
public final class Identity extends ModelObject<Identity> {
    private long id = NOT_STORED;
    private final String issuer;
    private final String accountName;
    private final Uri image;
    private final List<Mechanism> mechanismList;
    private static final Logger logger = LoggerFactory.getLogger(Identity.class);


    private Identity(long id, String issuer, String accountName, Uri image) {
        this.id = id;
        this.issuer = issuer;
        this.accountName = accountName;
        this.image = image;
        this.mechanismList = new SortedList<>();
    }

    /**
     * Adds the provided mechanism to this Identity, and therefore to the larger data model.
     * @param context The context that the mechanism is being added from.
     * @param builder An incomplete builder for a non stored mechanism.
     * @return The mechanism that has been added to the data model.
     * @throws MechanismCreationException If something went wrong when creating the mechanism.
     */
    public Mechanism addMechanism(Context context, Mechanism.PartialMechanismBuilder builder) throws MechanismCreationException {
        Mechanism mechanism = builder.setOwner(this).build();
        if (!mechanism.isStored() && !mechanismList.contains(mechanism)) {
            mechanism.save(context);
            mechanismList.add(mechanism);
        }
        return mechanism;
    }

    /**
     * Deletes the provided mechanism, and removes it from this Identity. Deletes this identity if
     * this results in this identity containing no mechanisms.
     * @param context The context that the mechanism is being deleted from.
     * @param mechanism The mechanism to delete.
     */
    public void removeMechanism(Context context, Mechanism mechanism) {
        mechanism.delete(context);
        mechanismList.remove(mechanism);

        if (mechanismList.isEmpty()) {
            RoboGuice.getInjector(context).getInstance(IdentityModel.class).removeIdentity(context, this);
        }
    }

    /**
     * Gets all of the mechanisms that belong to this Identity.
     * @return The list of mechanisms.
     */
    public List<Mechanism> getMechanisms() {
        return Collections.unmodifiableList(mechanismList);
    }

    /**
     * Gets the name of the IDP that issued this identity.
     * @return The name of the IDP.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the name of this Identity.
     * @return The account name if it has been assigned or an empty String.
     */
    public String getAccountName() {
        return accountName != null ? accountName : "";
    }

    /**
     * Gets the image for the IDP that issued this identity.
     * @return Non null {@link Uri} representing the path to the image, or null if not assigned.
     */
    public Uri getImage() {
        return image;
    }

    @Override
    public ArrayList<String> getOpaqueReference() {
        ArrayList<String> reference = new ArrayList<>();
        reference.add(issuer + ":" + accountName);
        return reference;
    }

    @Override
    public boolean consumeOpaqueReference(ArrayList<String> reference) {
        if (reference.size() > 0 && reference.get(0) != null &&
                reference.get(0).equals(issuer + ":" + accountName)) {
            reference.remove(0);
            return true;
        }
        return false;
    }

    @Override
    public boolean validate() {
        boolean valid = true;
        for (Mechanism mechanism : mechanismList) {
            valid = valid && mechanism.validate();
        }
        return isStored() && valid;
    }

    @Override
    public boolean isStored() {
        return id != NOT_STORED;
    }

    @Override
    public void save(Context context) {
        if (!isStored()) {
            id = RoboGuice.getInjector(context).getInstance(IdentityDatabase.class).addIdentity(this);
        } else {
            // TODO: handle updates
        }
    }

    @Override
    public void delete(Context context) {
        for (Mechanism mechanism : mechanismList) {
            mechanism.delete(context);
        }
        if (id != NOT_STORED) {
            RoboGuice.getInjector(context).getInstance(IdentityDatabase.class).deleteIdentity(id);
            id = NOT_STORED;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof Identity)) {
            return false;
        }
        Identity other = (Identity) object;
        return other.issuer.equals(issuer) && other.accountName.equals(accountName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuer, accountName); //TODO: insufficient API level
    }

    /**
     * Returns a builder for creating an Identity.
     * @return The Identity builder.
     */
    public static IdentityBuilder builder() {
        return new IdentityBuilder();
    }

    private void populateMechanisms(List<Mechanism.PartialMechanismBuilder> mechanismBuilders) {
        for (Mechanism.PartialMechanismBuilder mechanismBuilder : mechanismBuilders) {
            try {
                Mechanism mechanism = mechanismBuilder.setOwner(this).build();
                if (mechanism.isStored()) {
                    mechanismList.add(mechanism);
                } else {
                    logger.error("Tried to populate mechanism list with Mechanism that has not been stored.");
                }
            } catch (MechanismCreationException e) {
                logger.error("Something went wrong while loading Mechanism.", e);
            }
        }
        Collections.sort(mechanismList);
    }

    @Override
    public int compareTo(Identity another) {
        int compareIssuer = issuer.compareTo(another.issuer);
        if (compareIssuer == 0) {
            return accountName.compareTo(another.accountName);
        }
        return compareIssuer;
    }

    /**
     * Builder class responsible for producing Identities.
     */
    public static class IdentityBuilder {
        private long id = NOT_STORED;
        private String issuer;
        private String accountName;
        private Uri image;
        private List<Mechanism.PartialMechanismBuilder> mechanismBuilders = new ArrayList<>();

        /**
         * Sets the storage id of this Identity. Should not be set manually, or if the Identity is
         * not stored.
         * @param id The storage id.
         */
        public IdentityBuilder setId(long id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the IDP that issued this identity.
         * @param issuer The IDP name.
         */
        public IdentityBuilder setIssuer(String issuer) {
            this.issuer = issuer != null ? issuer : "";
            return this;
        }

        /**
         * Sets the name of the identity.
         * @param accountName The identity name.
         */
        public IdentityBuilder setAccountName(String accountName) {
            this.accountName = accountName != null ? accountName : "";
            return this;
        }

        /**
         * Sets the image for the IDP that issued this identity.
         * @param image A string that represents the image URI.
         */
        public IdentityBuilder setImage(String image) {
            this.image = image == null ? null : Uri.parse(image);
            return this;
        }

        /**
         * Sets the mechanisms that are currently associated with this Identity.
         * @param mechanismBuilders A list of incomplete mechanism builders.
         */
        public IdentityBuilder setMechanisms(List<Mechanism.PartialMechanismBuilder> mechanismBuilders) {
            this.mechanismBuilders = mechanismBuilders;
            return this;
        }

        /**
         * Produces the Identity object that was being constructed.
         * @return The identity.
         */
        public Identity build() {
            Identity result =  new Identity(id, issuer, accountName, image);
            result.populateMechanisms(mechanismBuilders);
            return result;
        }
    }
}