package com.example.authapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class Register extends AppCompatActivity {

    EditText Name,signUpEmail,signInPassword,confirmSignInPassword;
    Button btnGoLoginPage,btnSignUp;
    FirebaseAuth fAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Name = findViewById(R.id.Name);
        signUpEmail = findViewById(R.id.signUpEmail);
        signInPassword = findViewById(R.id.signInPassword);
        confirmSignInPassword = findViewById(R.id.confirmSignInPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        btnGoLoginPage = findViewById(R.id.btnGoLoginPage);

        fAuth = FirebaseAuth.getInstance();

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fullName = Name.getText().toString();
                String email = signUpEmail.getText().toString();
                String password = signInPassword.getText().toString();
                String confirmPassword = confirmSignInPassword.getText().toString();

                if(fullName.isEmpty()){
                    Name.setError("FullName is Required");
                    return;
                }
                if(email.isEmpty()){
                    signUpEmail.setError("Email is Required");
                    return;
                }
                if(password.isEmpty()){
                    signInPassword.setError("FullName is Required");
                    return;
                }
                if(confirmPassword.isEmpty()){
                    confirmSignInPassword.setError("FullName is Required");
                    return;
                }
                if(!password.equals(confirmPassword)){
                    confirmSignInPassword.setError("Password do not match");
                }

                Toast.makeText(Register.this, "Data Validate", Toast.LENGTH_SHORT).show();

                fAuth.createUserWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        startActivity(new Intent(getApplicationContext(),MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Register.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        
        btnGoLoginPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(),Login.class));
                finish();
            }
        });
    }
}