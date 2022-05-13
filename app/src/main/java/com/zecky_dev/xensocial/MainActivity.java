package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zecky_dev.xensocial.databinding.ActivityMainBinding;


public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPrefEditor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        // Initializing mAuth
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser!=null){
            Intent feedIntent = new Intent(MainActivity.this,FeedActivity.class);
            startActivity(feedIntent);
            finish();
        }
        sharedPreferences = this.getSharedPreferences("sharedPref",Context.MODE_PRIVATE);
        sharedPrefEditor = sharedPreferences.edit();
        binding.signUpBTNLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent signUpIntent = new Intent(MainActivity.this, EditSignupActivity.class);
                signUpIntent.putExtra("processType","signup");
                startActivity(signUpIntent);
            }
        });

        binding.loginBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String emailAddress = binding.emailAddressETLogin.getText().toString();
                String password = binding.passwordETLogin.getText().toString();
                if(!emailAddress.isEmpty() && !password.isEmpty()){
                    mAuth.signInWithEmailAndPassword(emailAddress,password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if(task.isSuccessful()){
                                        sharedPrefEditor.putString("loginEmail",emailAddress);
                                        sharedPrefEditor.putString("loginPass",password);
                                        sharedPrefEditor.commit();
                                        Toast.makeText(MainActivity.this, "Login is successful!", Toast.LENGTH_SHORT).show();
                                        Intent feedIntent = new Intent(MainActivity.this,FeedActivity.class);
                                        startActivity(feedIntent);
                                        finish();
                                    }
                                    else{
                                        Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }

            }
        });

    }
}