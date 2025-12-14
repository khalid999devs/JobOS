package com.jobos.android;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class JobOSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
