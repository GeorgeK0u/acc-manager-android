package com.example.accmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.example.accmanager.utils.ViewHandler;
import com.example.accmanager.utils.XmlHandler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StartingActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starting_activity);
        Init();
    }

    private void Init()
    {
        // set a scheduled activity switch to prevent switch delay
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(() -> CheckWhichActivity(), 1, TimeUnit.SECONDS);
    }

    private void CheckWhichActivity()
    {
        // check which starting activity
        if (XmlHandler.IsLocked()) {
            // go to login activity
            Intent GoToLoginActivity = new Intent(this, LoginActivity.class);
            startActivity(GoToLoginActivity);
        } else {
            ViewHandler.OnLogin();
            // go to main activity
            Intent GoToMainActivity = new Intent(this, MainActivity.class);
            startActivity(GoToMainActivity);
        }
    }
}