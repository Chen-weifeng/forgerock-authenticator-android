package com.forgerock.authenticator;

import android.app.Application;
import android.content.Context;

import com.forgerock.authenticator.storage.IdentityDatabase;
import com.google.inject.AbstractModule;

import roboguice.RoboGuice;

/**
 * Guice module for the ForgeRock Authenticator app.
 */
public class AuthGuiceModule extends AbstractModule {
    private final Context context;

    static {
        // Used to disable RoboBlender, which would cause crashes.
        RoboGuice.setUseAnnotationDatabases(false);
    }

    public AuthGuiceModule(Application application) {
        this.context = application.getBaseContext();
    }

    @Override
    protected void configure() {
        bind(IdentityDatabase.class).toInstance(new IdentityDatabase(context));
    }
}
