package com.appeteria.training.gupshup.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.appeteria.training.gupshup.Common.Util;
import com.appeteria.training.gupshup.MainActivity;
import com.appeteria.training.gupshup.MessageActivity;
import com.appeteria.training.gupshup.R;
import com.appeteria.training.gupshup.password.ResetPasswordActivity;
import com.appeteria.training.gupshup.signup.SignupActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private String email, password;
    private  View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword= findViewById(R.id.etPassword);

        progressBar = findViewById(R.id.progressBar);

    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser!=null)
        {

            FirebaseMessaging.getInstance ().getToken ()
                    .addOnCompleteListener ( task -> {
                        if (!task.isSuccessful ()) {
                            //Could not get FirebaseMessagingToken
                            return;
                        }
                        if (null != task.getResult ()) {
                            //Got FirebaseMessagingToken
                            String firebaseMessagingToken = Objects.requireNonNull (task.getResult());
                            //Use firebaseMessagingToken further
                            Util.updateDeviceToken(LoginActivity.this, firebaseMessagingToken);
                        }
                    } );

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();

        }
    }


    public void tvSignupClick(View v)
    {
        startActivity(new Intent(this, SignupActivity.class));
    }
    public void btnLoginClick(View v)
    {
        email = etEmail.getText().toString().trim();
        password= etPassword.getText().toString().trim();

        if(email.equals(""))
        {
            etEmail.setError(getString(R.string.enter_email));
        }
        else if (password.equals(""))
        {
            etPassword.setError(getString(R.string.enter_password));
        }
        else
        {
            if(Util.connectionAvailable(this)) {
                progressBar.setVisibility(View.VISIBLE);
                FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            onStart();
                            //startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            //finish();
                        } else {
                            Toast.makeText(LoginActivity.this, "Login Failed : " +
                                    task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
            else
            {
                startActivity(new Intent(LoginActivity.this, MessageActivity.class));
            }

        }


    }

    public  void tvResetPasswordClick(View view){
        startActivity(new Intent(LoginActivity.this , ResetPasswordActivity.class));
    }

    // Double back pressed in this LoginActivity will close the application
    private boolean isBackPressedOnce = false;
    @Override
    public void onBackPressed() {

        if (isBackPressedOnce) {
            super.onBackPressed();
            return;
        }

        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        isBackPressedOnce = true;

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isBackPressedOnce = false;
            }
        }, 2000);

    }

}























