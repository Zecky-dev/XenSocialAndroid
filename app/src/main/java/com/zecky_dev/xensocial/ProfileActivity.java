package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.juliomarcos.ImageViewPopUpHelper;
import com.squareup.picasso.Picasso;
import com.zecky_dev.xensocial.databinding.ActivityProfileBinding;

import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private String userFirstName,userLastName,userLikes,userPosts,userFollowers,userProfilePictureURL;
    private RecyclerView smallPostList_RW;
    private CustomPostAdapter customPostAdapter;
    private ArrayList<QueryDocumentSnapshot> queryDocumentSnapshots;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        smallPostList_RW = findViewById(R.id.smallPostList_RW);
        ImageViewPopUpHelper.enablePopUpOnClick(ProfileActivity.this,binding.profileImageIW);
        binding.settingsBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent editProfileActivity = new Intent(ProfileActivity.this,EditSignupActivity.class);
                editProfileActivity.putExtra("processType","edit");
                startActivity(editProfileActivity);
            }
        });
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();
        loadUserInformation();
        loadUserPosts();
    }


    private void loadUserInformation()
    {
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
                        Picasso.get().load(userProfilePictureURL).placeholder(R.drawable.progress_animation).into(binding.profileImageIW);
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
    }

    private void loadUserPosts()
    {
        int numberOfColumns = 2;
        String currentUserID = firebaseAuth.getCurrentUser().getUid();
        System.out.println(currentUserID);
        CollectionReference postInfoRef = firebaseFirestore.collection("post_info");
        Query idQuery = postInfoRef.whereEqualTo("PostOwnerID",currentUserID);
        idQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    queryDocumentSnapshots = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        queryDocumentSnapshots.add(document);
                    }
                    binding.postCountTW.setText(String.valueOf(queryDocumentSnapshots.size()));
                    smallPostList_RW.setLayoutManager(new GridLayoutManager(ProfileActivity.this,numberOfColumns));
                    customPostAdapter = new CustomPostAdapter(ProfileActivity.this,queryDocumentSnapshots);
                    smallPostList_RW.setAdapter(customPostAdapter);
                }
            }
        });
    }

}