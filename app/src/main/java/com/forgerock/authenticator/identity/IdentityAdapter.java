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

package com.forgerock.authenticator.identity;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.forgerock.authenticator.R;
import com.forgerock.authenticator.storage.IdentityModel;

import java.util.List;

import roboguice.RoboGuice;

/**
 * Class for linking the complete list of Identities with a series of layouts which display each one.
 */
public class IdentityAdapter extends BaseAdapter {
    private final IdentityModel identityModel;
    private final LayoutInflater mLayoutInflater;
    private List<Identity> identityList;

    /**
     * Creates the adapter, and finds the data model.
     */
    public IdentityAdapter(Context context) {
        identityModel = RoboGuice.getInjector(context).getInstance(IdentityModel.class);
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        identityList = identityModel.getIdentities();

    }

    @Override
    public int getCount() {
        return identityList.size();
    }

    @Override
    public Identity getItem(int position) {
        return identityList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.identitycell, parent, false);
        }

        Identity identity = getItem(position);
        ((IdentityLayout) convertView).bind(identity);
        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        identityList = identityModel.getIdentities();
        super.notifyDataSetChanged();
    }
}
