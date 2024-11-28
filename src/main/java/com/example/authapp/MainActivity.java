package com.example.authapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    TextView verifyText;
    Button btnVerify;
    Button getBtnVerify;
    FirebaseAuth auth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        verifyText = findViewById(R.id.textViewVerify);
        btnVerify = findViewById(R.id.btnVerify);
        getBtnVerify = findViewById(R.id.btnCheckVerify);

        auth = FirebaseAuth.getInstance();

        if(!auth.getCurrentUser().isEmailVerified()){
            btnVerify.setVisibility(View.VISIBLE);
            verifyText.setVisibility(View.VISIBLE);
            getBtnVerify.setVisibility(View.VISIBLE);
        }

        btnVerify.setOnClickListener(view -> {
            if (auth.getCurrentUser() != null) {
                auth.getCurrentUser().sendEmailVerification()
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(MainActivity.this, "Verification email sent. Please check your inbox.", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this, "Failed to send verification email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(MainActivity.this, "No user signed in.", Toast.LENGTH_SHORT).show();
            }
        });

        getBtnVerify.setOnClickListener(view -> {
            if (auth.getCurrentUser() != null) {
                auth.getCurrentUser().reload().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (auth.getCurrentUser().isEmailVerified()) {
                            Toast.makeText(MainActivity.this, "Email verified!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, NextActivity.class));
                            finish();
                        } else {
                            Toast.makeText(MainActivity.this, "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Failed to refresh user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(MainActivity.this, "No user signed in.", Toast.LENGTH_SHORT).show();
            }
        });


    }
}