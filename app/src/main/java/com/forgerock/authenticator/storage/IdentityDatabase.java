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

package com.forgerock.authenticator.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.forgerock.authenticator.identity.Identity;
import com.forgerock.authenticator.mechanisms.CoreMechanismFactory;
import com.forgerock.authenticator.mechanisms.base.Mechanism;
import com.forgerock.authenticator.mechanisms.MechanismCreationException;
import com.forgerock.authenticator.notifications.Notification;
import com.forgerock.authenticator.notifications.PushNotification;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Data Access Object which can store and load both Identities and Mechanisms. Encapsulates the
 * specific storage mechanism.
 */
public class IdentityDatabase {
    /** The name of the table the identities are stored in */
    static final String IDENTITY_TABLE_NAME = "identity";
    /**The name of the table the mechanisms are stored in */
    static final String MECHANISM_TABLE_NAME = "mechanism";
    /**The name of the table the notifications are stored in */
    static final String NOTIFICATION_TABLE_NAME = "notification";

    // Identity columns
    /** The IDP name column */
    static final String ISSUER = "issuer";
    /** The identity name column */
    static final String ACCOUNT_NAME = "accountName";
    /** The IDP image column */
    static final String IMAGE = "image";

    // Mechanism columns
    /** The IDP name column (Foreign key) */
    static final String ID_ISSUER = "idIssuer";
    /** The identity name column (Foreign key) */
    static final String ID_ACCOUNT_NAME = "idAccountName";
    /** The mechanism type column */
    static final String TYPE = "type";
    /** The mechanism version column */
    static final String VERSION = "version";
    /** The mechanism options column */
    static final String OPTIONS = "options";
    /** The mechanism uid generated if the mechanism can receive notifications */
    static final String MECHANISM_UID = "mechanismUID";

    // Notification columns
    /** The time that the notification was received */
    static final String ADDED_TIME = "timeAdded";
    /** The time that the notification will expire */
    static final String EXPIRY_TIME = "timeExpired";
    /** The data that is relevant to the notification (e.g. messageId) */
    static final String DATA = "data";
    /** Whether the notification was successful or not, for historical purposes */
    static final String APPROVED = "approved";
    /** Whether the notification has been handled, for historical purposes */
    static final String PENDING = "pending";


    private final Gson gson = new Gson();
    private final SQLiteDatabase database;
    private final CoreMechanismFactory coreMechanismFactory;
    private static final Logger logger = LoggerFactory.getLogger(IdentityDatabase.class);

    /**
     * Creates a connection to the database using the provided Context.
     * @param context The context that requested the connection.
     */
    public IdentityDatabase(Context context) {
        DatabaseOpenHelper databaseOpeHelper = new DatabaseOpenHelper(context);
        database = databaseOpeHelper.getWritableDatabase();
        coreMechanismFactory = new CoreMechanismFactory();
    }

    /**
     * Loads the complete list of Identities, loaded with the mechanisms and notifications from the database.
     * @return The complete set of data.
     */
    public List<Identity> getModel() {
        List<Identity.IdentityBuilder> identityBuilders = getIdentityBuilders();

        List<Identity> identities = new ArrayList<>();

        for (Identity.IdentityBuilder identityBuilder : identityBuilders) {
            identities.add(identityBuilder.build());
        }

        return identities;
    }

    /**
     * Add the identity to the database.
     * @param id The identity to add.
     */
    public long addIdentity(Identity id) {
        String issuer = id.getIssuer();
        String accountName = id.getAccountName();
        String image = id.getImage() == null ? null : id.getImage().toString();

        ContentValues values = new ContentValues();
        values.put(ISSUER, issuer);
        values.put(ACCOUNT_NAME, accountName);
        values.put(IMAGE, image);

        long rowId = database.insert(IDENTITY_TABLE_NAME, null, values);
        return rowId;
    }

    /**
     * Add the mechanism to the database. If the owning identity is not yet stored, store that as well.
     * @param mechanism The mechanism to store.
     */
    public long addMechanism(Mechanism mechanism) {
        String issuer = mechanism.getOwner().getIssuer();
        String accountName = mechanism.getOwner().getAccountName();
        String type = mechanism.getInfo().getMechanismString();
        int version = mechanism.getVersion();
        String options = gson.toJson(mechanism.asMap());

        ContentValues values = new ContentValues();
        values.put(ID_ISSUER, issuer);
        values.put(ID_ACCOUNT_NAME, accountName);
        values.put(TYPE, type);
        values.put(VERSION, version);
        values.put(OPTIONS, options);
        values.put(MECHANISM_UID, mechanism.getMechanismUID());

        long rowId = database.insert(MECHANISM_TABLE_NAME, null, values);
        return rowId;
    }

    /**
     * Add the notification to the database.
     * @param notification The notification to store.
     */
    public long addNotification(Notification notification) {
        long timeAdded = notification.getTimeAdded().getTimeInMillis();
        long timeExpired = notification.getTimeExpired().getTimeInMillis();
        int wasApproved = notification.wasApproved() ? 1 : 0;
        int isPending = notification.isPending() ? 1 : 0;
        int mechanismUID = notification.getMechanism().getMechanismUID();
        String data = gson.toJson(notification.getData());

        ContentValues values = new ContentValues();
        values.put(ADDED_TIME, timeAdded);
        values.put(EXPIRY_TIME, timeExpired);
        values.put(APPROVED, wasApproved);
        values.put(MECHANISM_UID, mechanismUID);
        values.put(DATA, data);
        values.put(PENDING, isPending);

        long rowId = database.insert(NOTIFICATION_TABLE_NAME, null, values);
        return rowId;
    }

    /**
     * Update the mechanism in the database. Does not create it if it does not exist.
     * @param mechanismId The id of the mechanism to update.
     * @param mechanism The mechanism to update it with.
     */
    public void updateMechanism(long mechanismId, Mechanism mechanism) {
        ContentValues values = new ContentValues();
        String options = gson.toJson(mechanism.asMap());
        values.put(OPTIONS, options);
        String[] selectionArgs = { Long.toString(mechanismId) };
        database.update(MECHANISM_TABLE_NAME, values, "rowId = ?", selectionArgs);
    }

    /**
     * Update the notification in the database. Does not create it if it does not exist.
     * @param notificationId The id of the notification to update.
     * @param notification The notification to update it with.
     */
    public void updateNotification(long notificationId, Notification notification) {
        ContentValues values = new ContentValues();

        int wasApproved = notification.wasApproved() ? 1 : 0;
        int isPending = notification.isPending() ? 1 : 0;

        values.put(PENDING, isPending);
        values.put(APPROVED, wasApproved);
        String[] selectionArgs = { Long.toString(notificationId) };
        database.update(NOTIFICATION_TABLE_NAME, values, "rowId = ?", selectionArgs);
    }

    /**
     * Delete the mechanism uniquely identified by an id.
     * @param mechanismId The id of the mechanism to delete.
     */
    public void deleteMechanism(long mechanismId) {
        database.delete(MECHANISM_TABLE_NAME, "rowId = " + mechanismId, null);
    }

    /**
     * Delete the identity that was passed in.
     * @param identityId The if of the identity to delete.
     */
    public void deleteIdentity(long identityId) {
        database.delete(IDENTITY_TABLE_NAME, "rowId = " + identityId, null);
    }

    /**
     * Delete the notification uniquely identified by an id.
     * @param notificationId The id of the notification to delete.
     */
    public void deleteNotification(long notificationId) {
        database.delete(NOTIFICATION_TABLE_NAME, "rowId = " + notificationId, null);
    }

    private List<Identity.IdentityBuilder> getIdentityBuilders() {
        Cursor cursor = database.rawQuery("SELECT rowid, * FROM " + IDENTITY_TABLE_NAME + " ORDER BY "
                + ISSUER + " ASC, " + ACCOUNT_NAME + " ASC", null);
        List<Identity.IdentityBuilder> result = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Identity.IdentityBuilder newIdentityBuilder = cursorToIdentityBuilder(cursor);
            result.add(newIdentityBuilder);
            cursor.moveToNext();
        }
        return result;
    }

    private Identity.IdentityBuilder cursorToIdentityBuilder(Cursor cursor) {
        int rowid = cursor.getInt(cursor.getColumnIndex("rowid"));
        String issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
        String accountName = cursor.getString(cursor.getColumnIndex(ACCOUNT_NAME));
        String image = cursor.getString(cursor.getColumnIndex(IMAGE));

        List<Mechanism.PartialMechanismBuilder> mechanismBuilders = getMechanismBuilders(issuer, accountName);

        Identity.IdentityBuilder identityBuilder = Identity.builder()
                .setIssuer(issuer)
                .setAccountName(accountName)
                .setImage(image)
                .setId(rowid)
                .setMechanisms(mechanismBuilders);
        return identityBuilder;
    }

    private List<Mechanism.PartialMechanismBuilder> getMechanismBuilders(String issuer, String accountName) {
        String[] selectionArgs = { issuer, accountName };

        Cursor cursor = database.rawQuery("SELECT rowid, * FROM " + MECHANISM_TABLE_NAME +
                " WHERE " + ID_ISSUER + " = ? AND " + ID_ACCOUNT_NAME + " = ?", selectionArgs);
        List<Mechanism.PartialMechanismBuilder> result = new ArrayList<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            try {
                result.add(cursorToMechanismBuilder(cursor));
            } catch (MechanismCreationException e) {
                logger.error("Failed to load mechanism. This may be caused by invalid data, or data " +
                        "that has not been upgraded.", e);
                // Don't add the mechanism that failed to load.
            }
            cursor.moveToNext();
        }
        return result;
    }

    private Mechanism.PartialMechanismBuilder cursorToMechanismBuilder(Cursor cursor) throws MechanismCreationException {
        String type = cursor.getString(cursor.getColumnIndex(TYPE));
        int version = cursor.getInt(cursor.getColumnIndex(VERSION));
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> options =
                gson.fromJson(cursor.getString(cursor.getColumnIndex(OPTIONS)), mapType);

        int mechanismUID = cursor.getInt(cursor.getColumnIndex(MECHANISM_UID));

        List<Notification.NotificationBuilder> notificationBuilders = getNotificationBuilders(mechanismUID);

        Mechanism.PartialMechanismBuilder mechanismBuilder = coreMechanismFactory.restoreFromParameters(type, version, options);
        mechanismBuilder.setId(cursor.getLong(cursor.getColumnIndex("rowid")))
                .setMechanismUID(mechanismUID)
                .setNotifications(notificationBuilders);
        return mechanismBuilder;
    }

    private List<Notification.NotificationBuilder> getNotificationBuilders(int mechanismUid) {
        if (mechanismUid == -1) {
            return new ArrayList<>();
        }
        String[] selectionArgs = { Integer.toString(mechanismUid) };

        Cursor cursor = database.rawQuery("SELECT rowid, * FROM " + NOTIFICATION_TABLE_NAME +
                " WHERE " + MECHANISM_UID + " = ?", selectionArgs);
        List<Notification.NotificationBuilder> result = new ArrayList<>();
        cursor.moveToFirst();

        while (!cursor.isAfterLast()) {
            try {
                result.add(cursorToNotificationBuilder(cursor));
            } catch (MechanismCreationException e) {
                logger.error("Failed to load notification. This may be caused by invalid data, or data " +
                        "that has not been upgraded.", e);
                // Don't add the notification that failed to load.
            }
            cursor.moveToNext();
        }
        return result;
    }

    private Notification.NotificationBuilder cursorToNotificationBuilder(Cursor cursor) throws MechanismCreationException {
        int rowid = cursor.getInt(cursor.getColumnIndex("rowid"));
        Calendar addedTime = Calendar.getInstance();
        addedTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(ADDED_TIME)));
        Calendar expiryTime = Calendar.getInstance();
        expiryTime.setTimeInMillis(cursor.getLong(cursor.getColumnIndex(EXPIRY_TIME)));
        boolean approved = cursor.getLong(cursor.getColumnIndex(APPROVED)) == 1;
        boolean pending = cursor.getLong(cursor.getColumnIndex(PENDING)) == 1;
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();
        Map<String, String> data =
                gson.fromJson(cursor.getString(cursor.getColumnIndex(DATA)), mapType);

        // TODO: When more types of Notification are possible, get base builder from Mechanism, or possibly use a factory.
        PushNotification.PushNotificationBuilder notificationBuilder = PushNotification.builder()
                .setApproved(approved)
                .setTimeAdded(addedTime)
                .setTimeExpired(expiryTime)
                .setData(data)
                .setId(rowid)
                .setPending(pending);
        return notificationBuilder;
    }
}
