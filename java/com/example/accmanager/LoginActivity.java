package com.example.accmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.accmanager.utils.ViewHandler;
import com.example.accmanager.utils.XmlHandler;

public class LoginActivity extends AppCompatActivity
{
    private EditText CodeInput;
    private Button EnterBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        Init();
    }

    private void Init()
    {
        CodeInput = findViewById(R.id.codeInput);
        EnterBtn = findViewById(R.id.enterBtn);
        EnterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                // check if code is correct
                String code = CodeInput.getText().toString();
                String validCode = XmlHandler.GetLockCode();
                if (!code.equals(validCode))
                {
                    Toast.makeText(getApplicationContext(), "Invalid code", Toast.LENGTH_SHORT).show();
                    return;
                }
                // valid code
                ViewHandler.OnLogin();
                // go to main activity
                Intent GoToMainActivity = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(GoToMainActivity);
                finish();
            }
        });
    }
}