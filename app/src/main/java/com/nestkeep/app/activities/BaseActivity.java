package com.nestkeep.app.activities;

import androidx.appcompat.app.AppCompatActivity;
import com.nestkeep.app.utils.SessionManager;
import android.os.Bundle;

// Base class for all authenticated activities.
// Keeps the session timestamp fresh while the user is active,
// so the 30-minute timeout only counts time spent outside the app.
public abstract class BaseActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sessionManager.updateLastActive();
    }
}
