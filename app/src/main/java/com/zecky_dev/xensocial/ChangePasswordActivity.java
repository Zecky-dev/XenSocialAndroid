package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zecky_dev.xensocial.databinding.ActivityChangePasswordBinding;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private ActivityChangePasswordBinding binding;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private String oldPass,newPassAgain,newPass,loginEmail,loginPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChangePasswordBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        SharedPreferences sharedPreferences = this.getSharedPreferences("sharedPref", Context.MODE_PRIVATE);
        loginEmail = sharedPreferences.getString("loginEmail","");
        loginPass = sharedPreferences.getString("loginPass","");

        binding.changePassBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getInputValues();
                if(checkInputRequirements(oldPass,newPass,newPassAgain,loginPass)){
                    updatePassword(loginEmail,loginPass,newPass);
                }
            }
        });


    }

    private void updatePassword(String loginEmail,String loginPass,String newPassword)
    {
        AuthCredential credential = EmailAuthProvider
                .getCredential(loginEmail,loginPass);
        firebaseUser.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            AlertDialog.Builder changePassDialog = new AlertDialog.Builder(ChangePasswordActivity.this);
                            changePassDialog.setTitle("Are you sure?");
                            changePassDialog.setMessage("If you change your password you must re-sign with your new password.");
                            changePassDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    firebaseUser
                                            .updatePassword(newPassword)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(ChangePasswordActivity.this, "Password successfully updated!", Toast.LENGTH_SHORT).show();
                                                    firebaseAuth.signOut();
                                                    finish();
                                                    Intent loginIntent = new Intent(ChangePasswordActivity.this,MainActivity.class);
                                                    startActivity(loginIntent);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(ChangePasswordActivity.this, "Password update failed!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            });
                            changePassDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                }
                            });
                            changePassDialog.create().show();
                        }
                    }
                });
    }

    private boolean checkInputRequirements(String oldPassInput,String newPassInput,String newPassAgainInput,String loginPass)
    {
        // Boş olup olmadıkları kontrol edildi!
        if(!oldPassInput.isEmpty() && !newPassInput.isEmpty() && !newPassAgainInput.isEmpty()){
            // Girilen yeni şifrenin tekrar yazılan yeni şifreyle aynı olup olmadığı kontrol edildi
            if(newPassInput.equals(newPassAgainInput)){
                // Girilen yeni şifrenin yeterince güçlü olup olmadığı kontrol edildi
                if(isValidPassword(newPassInput)){
                   // Eski şifrenin giriş şifresine eşit olup olmadığı kontrol edildi
                    if(oldPassInput.equals(loginPass)){
                        // Eski şifrenin yeni şifreye eşit olup olmadığı kontrol edildi
                        if(!oldPassInput.equals(newPassInput)){
                            return true;
                        }
                        else{
                            Toast.makeText(this, "Your old password cannot be same with new password!", Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                    else{
                        Toast.makeText(this, "Your old password is wrong!", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                else{
                    Toast.makeText(this, "Your new pass is weak!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            else{
                Toast.makeText(this, "New pass != New pass again", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        else{
            Toast.makeText(this, "Please fill blank area(s)!", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void getInputValues()
    {
        oldPass = binding.oldPasswordET.getText().toString();
        newPassAgain = binding.newPassworAgainET.getText().toString();
        newPass = binding.newPasswordET.getText().toString();
    }

    public static boolean isValidPassword(final String password) {
        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[.#?!@$%^&*-]).{8,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }



}