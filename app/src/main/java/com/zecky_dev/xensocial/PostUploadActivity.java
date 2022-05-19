package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.juliomarcos.ImageViewPopUpHelper;
import com.squareup.picasso.Picasso;
import com.zecky_dev.xensocial.databinding.ActivityPostUploadBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

public class PostUploadActivity extends AppCompatActivity {

    private static final String TAG = "";
    private ActivityPostUploadBinding binding;
    private final int GALLERY_PERMISSION_REQUEST_CODE = 99;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;
    private FirebaseUser currentUser;
    private final int GALLERY_PICK_IMAGE_CODE = 101;
    private Bitmap postImageBitmap;
    private boolean postImageSelected = false,locationActivated = false;
    private String currentCityName="",currentNeighborhood="",currentCountryName="";
    private String postTitle,postComment,postOwnerID,fullAddress,postImageURL;
    private StorageReference postImageReference,storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostUploadBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        ImageViewPopUpHelper.enablePopUpOnClick(PostUploadActivity.this,binding.postImageIW);
        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        // Button click listeners
        // Add post image button listener
        binding.addPostImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkGalleryPermission()){
                    pickImageFromGallery();
                }
                else{
                    Toast.makeText(PostUploadActivity.this, "", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Add location button listener
        binding.addPostLocationBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkLocationPermission()){
                    LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    double longitude = location.getLongitude();
                    double latitude = location.getLatitude();
                    try {

                        Geocoder geo = new Geocoder(PostUploadActivity.this, Locale.getDefault());
                        List<Address> addresses = geo.getFromLocation(latitude, longitude, 1);
                        if (addresses.isEmpty()) {
                            Log.e("Location Error","location addrress is empty");
                        }
                        else {
                            binding.locationInfoTW.setVisibility(View.VISIBLE);
                            currentCityName = addresses.get(0).getAdminArea();
                            currentNeighborhood = addresses.get(0).getSubLocality();
                            currentCountryName = addresses.get(0).getCountryName();
                            fullAddress = currentNeighborhood + " mahallesi, " + currentCityName + "," + currentCountryName;
                                binding.locationLayout.setVisibility(View.VISIBLE);
                                binding.locationInfoTW.setText(fullAddress);
                                locationActivated = true;

                        }

                    }catch (Exception e){
                        Log.e("Location error","location finding error");
                    }
                }
            }
        });
        // Delete location button listener
        binding.deleteLocationInfoBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationActivated = false;
                fullAddress = "";
                binding.locationLayout.setVisibility(View.GONE);
            }
        });
        // Delete post image button listener
        binding.deletePostImageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.postImageIW.setImageResource(R.drawable.no_image_selected);
                binding.postImageIW.setVisibility(View.GONE);
                binding.deletePostImageBTN.setVisibility(View.GONE);
                postImageSelected = false;
            }
        });
        // Share post button listener
        binding.shareBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sharePost();
            }
        });

    }


    private boolean checkGalleryPermission()
    {
        if(ContextCompat.checkSelfPermission(PostUploadActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(PostUploadActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)){
                AlertDialog.Builder galleryPermissionDialog = new AlertDialog.Builder(PostUploadActivity.this);
                galleryPermissionDialog.setTitle("Permission needed!");
                galleryPermissionDialog.setMessage("This app requires gallery permission to access your gallery");
                galleryPermissionDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestGalleryPermission();
                    }
                });
                galleryPermissionDialog.show();
            }
            else{
                // request permission
                requestGalleryPermission();
            }
        }
        else{
            return true;
        }
        return false;
    }

    public boolean checkLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(PostUploadActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(PostUploadActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)){
                AlertDialog.Builder locationPermissionDialog = new AlertDialog.Builder(PostUploadActivity.this);
                locationPermissionDialog.setTitle("Permission needed!");
                locationPermissionDialog.setMessage("This app requires location permission to access your location service");
                locationPermissionDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // request permission
                        requestLocationPermission();
                    }
                });
                locationPermissionDialog.setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                locationPermissionDialog.create().show();
                return false;
            }
            else{
                requestLocationPermission();
            }
        }
        else{
            return true;
        }
        return false;
    }

    private void requestGalleryPermission()
    {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(PostUploadActivity.this,permissions,GALLERY_PERMISSION_REQUEST_CODE);
    }

    private void requestLocationPermission()
    {
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION};
        ActivityCompat.requestPermissions(PostUploadActivity.this,permissions,LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void sharePost()
    {
        UUID postID = UUID.randomUUID();
        postTitle = binding.postTitleET.getText().toString();
        postComment = binding.postCommentET.getText().toString();
        postOwnerID = currentUser.getUid();
        Map<String,Object> postInfo = new HashMap<>();
        if(!checkInputsAreEmpty(postTitle,postComment)){
            binding.postDetailsLayout.setVisibility(View.GONE);
            binding.progressBarLayout.setVisibility(View.VISIBLE);
            postInfo.put("PostID",postID.toString());
            postInfo.put("PostOwnerID",postOwnerID);
            postInfo.put("PostTitle",postTitle);
            postInfo.put("PostComment",postComment);
            if(locationActivated)
            {
                postInfo.put("PostAddress",fullAddress);
            }
            if(postImageSelected){
                StorageReference storageReference = firebaseStorage.getReference();
                StorageReference postImageReference = storageReference.child("post_images/"+postID.toString()+".jpg");
                byte[] postImageInByteArray = compressImageAndConvertByteArray(postImageBitmap);
                UploadTask uploadTask = postImageReference.putBytes(postImageInByteArray);
                uploadTask
                        .addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if(task.isSuccessful()){
                           postImageReference.getDownloadUrl()
                                   .addOnSuccessListener(new OnSuccessListener<Uri>() {
                               @Override
                               public void onSuccess(Uri uri) {
                                   postInfo.put("PostImageURL",uri.toString());
                                   firebaseFirestore.collection("post_info").document(postID.toString()).set(postInfo)
                                           .addOnSuccessListener(new OnSuccessListener<Void>() {
                                               @Override
                                               public void onSuccess(Void unused) {
                                                   Toast.makeText(PostUploadActivity.this, "Post uploaded!", Toast.LENGTH_SHORT).show();
                                                   binding.postDetailsLayout.setVisibility(View.VISIBLE);
                                                   binding.progressBarLayout.setVisibility(View.GONE);
                                               }
                                           })
                                           .addOnFailureListener(new OnFailureListener() {
                                               @Override
                                               public void onFailure(@NonNull Exception e) {
                                                   Toast.makeText(PostUploadActivity.this, "Post upload failure!", Toast.LENGTH_SHORT).show();
                                                   binding.postDetailsLayout.setVisibility(View.VISIBLE);
                                                   binding.progressBarLayout.setVisibility(View.GONE);
                                               }
                                           });
                               }
                           })
                                   .addOnFailureListener(new OnFailureListener() {
                                       @Override
                                       public void onFailure(@NonNull Exception e) {

                                       }
                                   });
                        }
                    }
                });
            }
            else{
                postInfo.put("PostImageURL",null);
                firebaseFirestore.collection("post_info").document(postID.toString()).set(postInfo)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Toast.makeText(PostUploadActivity.this, "Post uploaded!", Toast.LENGTH_SHORT).show();
                                binding.postDetailsLayout.setVisibility(View.VISIBLE);
                                binding.progressBarLayout.setVisibility(View.GONE);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(PostUploadActivity.this, "Post upload failure!", Toast.LENGTH_SHORT).show();
                                binding.postDetailsLayout.setVisibility(View.VISIBLE);
                                binding.progressBarLayout.setVisibility(View.GONE);
                            }
                        });
            }
        }
        else{
            Toast.makeText(this, "Please fill post title and post comment!", Toast.LENGTH_SHORT).show();
        }
    }


    // Check results of request methods
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(String permission:permissions){
            System.out.println(permission);
        }
    }

    // Start an intent to go gallery
    private void pickImageFromGallery()
    {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent,GALLERY_PICK_IMAGE_CODE);
    }


    // Gallery intent onActivityResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == GALLERY_PICK_IMAGE_CODE && resultCode == Activity.RESULT_OK){
            if(data!=null){
                postImageSelected = true;
                try {
                    postImageBitmap = getCorrectlyOrientedImage(PostUploadActivity.this,data.getData(),binding.postImageIW.getMaxWidth());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    postImageBitmap.compress(Bitmap.CompressFormat.JPEG,50,baos);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(postImageBitmap!=null){
                    binding.deletePostImageBTN.setVisibility(View.VISIBLE);
                    binding.postImageIW.setVisibility(View.VISIBLE);
                    binding.postImageIW.setImageBitmap(postImageBitmap);
                }
                else{
                    Toast.makeText(this, "Post image bitmap is null!", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(this, "An error occurred when image selecting.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // Fix selected image's rotation problem
    public static int getOrientation(Context context, Uri photoUri) {

        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return 90;  //Assuming it was taken portrait
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
    }
    public static Bitmap getCorrectlyOrientedImage(Context context, Uri photoUri, int maxWidth)
            throws IOException {

        InputStream is = context.getContentResolver().openInputStream(photoUri);
        BitmapFactory.Options dbo = new BitmapFactory.Options();
        dbo.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(is, null, dbo);
        is.close();


        int rotatedWidth, rotatedHeight;
        int orientation = getOrientation(context, photoUri);

        if (orientation == 90 || orientation == 270) {
            Log.d("ImageUtil", "Will be rotated");
            rotatedWidth = dbo.outHeight;
            rotatedHeight = dbo.outWidth;
        } else {
            rotatedWidth = dbo.outWidth;
            rotatedHeight = dbo.outHeight;
        }

        Bitmap srcBitmap;
        is = context.getContentResolver().openInputStream(photoUri);
        Log.d("ImageUtil", String.format("rotatedWidth=%s, rotatedHeight=%s, maxWidth=%s",
                rotatedWidth, rotatedHeight, maxWidth));
        if (rotatedWidth > maxWidth || rotatedHeight > maxWidth) {
            float widthRatio = ((float) rotatedWidth) / ((float) maxWidth);
            float heightRatio = ((float) rotatedHeight) / ((float) maxWidth);
            float maxRatio = Math.max(widthRatio, heightRatio);
            Log.d("ImageUtil", String.format("Shrinking. maxRatio=%s",
                    maxRatio));

            // Create the bitmap from file
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = (int) maxRatio;
            srcBitmap = BitmapFactory.decodeStream(is, null, options);
        } else {
            Log.d("ImageUtil", String.format("No need for Shrinking. maxRatio=%s",
                    1));

            srcBitmap = BitmapFactory.decodeStream(is);
            Log.d("ImageUtil", String.format("Decoded bitmap successful"));
        }
        is.close();
        if (orientation > 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(orientation);

            srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(),
                    srcBitmap.getHeight(), matrix, true);
        }

        return srcBitmap;
    }

    // Check inputs are empty or not
    private boolean checkInputsAreEmpty(String postTitle,String postComment)
    {
        // If any edittext input is empty returns true
        if(postTitle.isEmpty() || postComment.isEmpty()) {
            return true;
        }
        else{
            return false;
        }
    }

    private byte[] compressImageAndConvertByteArray(Bitmap postImageBitmap)
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        postImageBitmap.compress(Bitmap.CompressFormat.JPEG,50,baos);
        return baos.toByteArray();
    }






}