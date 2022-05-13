package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;
import com.zecky_dev.xensocial.databinding.ActivityProfileBinding;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private String userFirstName,userLastName,userLikes,userPosts,userFollowers,userProfilePictureURL;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        DocumentReference docRef = firebaseFirestore
                .collection("user_info")
                .document(firebaseUser.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        userProfilePictureURL = (String)document.get("ProfilePictureURL");
                        userFirstName = (String) document.get("FirstName");
                        userLastName = (String) document.get("LastName");
                        Picasso.get().load(userProfilePictureURL).into(binding.profileImageIW);
                        binding.firstLastNameTW.setText(userFirstName + " " + userLastName);
                     }
                    else{
                        Toast.makeText(ProfileActivity.this,"Document not exists!",Toast.LENGTH_SHORT);
                    }
                }
                else{
                    Toast.makeText(ProfileActivity.this, "Error occured when getting document!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.settingsBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editProfileActivity = new Intent(ProfileActivity.this,EditSignupActivity.class);
                editProfileActivity.putExtra("processType","edit");
                startActivity(editProfileActivity);
            }
        });

    }
}