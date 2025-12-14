package com.jobos.android;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class JobOSApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
        
        FirebaseDatabase.getInstance(
            "https://jobos-4e21e-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).setPersistenceEnabled(true);
    }
}
