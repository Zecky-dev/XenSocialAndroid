package com.zecky_dev.xensocial;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.type.DateTime;
import com.squareup.picasso.Picasso;
import com.zecky_dev.xensocial.databinding.ActivitySignupBinding;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class EditSignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirebaseFireStore;
    private FirebaseStorage mFirebaseStorage;
    private ActivitySignupBinding binding;
    private String firstName_str, lastName_str, emailAddress_str, password_str;
    private String processType;
    private char gender_chr;
    private int birthday_day, birthday_month, birthday_year;
    private int GALLERY_PERMISSION_REQUEST_CODE = 101, GALLERY_PICK_IMAGE = 99;
    private Uri selectedImageURI;
    private Spinner genderSpinner;
    private DatePickerDialog mDatePicker;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String imageURL;
        // Viewbinding
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        // Getting intent and it's extras
        Intent intent = getIntent();
        processType = intent.getStringExtra("processType");
        // Firebase initializations
        mAuth = FirebaseAuth.getInstance();
        mFirebaseFireStore = FirebaseFirestore.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        // Gender spinner and it's listeners
        genderSpinner = binding.genderSelectSP;
        ArrayAdapter<CharSequence> genderSelectAdapter = ArrayAdapter.createFromResource(EditSignupActivity.this, R.array.genders, android.R.layout.simple_spinner_item);
        genderSelectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderSelectAdapter);
        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0) {
                    gender_chr = 'X';
                } else if (i == 1) {
                    gender_chr = 'M';
                } else if (i == 2) {
                    gender_chr = 'F';
                } else {
                    gender_chr = 'O';
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                gender_chr = 'X';
            }
        });
        // Birthday selecting and what will happen when a date value is selected
        binding.selectBirthdayBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar mCurrentDate = Calendar.getInstance();
                int mYear = mCurrentDate.get(Calendar.YEAR);
                int mMonth = mCurrentDate.get(Calendar.MONTH);
                int mDay = mCurrentDate.get(Calendar.DAY_OF_MONTH);
                mDatePicker = new DatePickerDialog(EditSignupActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        birthday_year = i;
                        birthday_month = i1;
                        birthday_day = i2;
                        binding.selectBirthdayBTN.setText("Selected Date: " + birthday_day + "." + birthday_month + "." + birthday_year);
                    }
                }, mYear, mMonth, mDay);
                mDatePicker.setTitle("Select Date");
                mDatePicker.show();
            }
        });
        // Selecting image from gallery if gallery permission is given
        binding.selectImageIW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkPermissions()) {
                    pickImageFromGallery();
                }
            }
        });

        // When click signup button
        if (processType.equals("signup")) {
            binding.signUpTW.setText("Sign up");
            binding.saveChangesBTN.setVisibility(View.GONE);
            binding.changePasswordBTN.setVisibility(View.GONE);
            binding.signupBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(checkInputValidations(processType)){
                            mAuth.createUserWithEmailAndPassword(emailAddress_str, password_str)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(EditSignupActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                                                FirebaseUser currentUser = mAuth.getCurrentUser();
                                                String currentUserID = currentUser.getUid();
                                                String birthDay_str = birthday_day + "." + birthday_month + "." + birthday_year;
                                                String gender_str = decodeUserGender(gender_chr);
                                                if(currentUser!=null){
                                                    StorageReference storageReference = mFirebaseStorage.getReference();
                                                    StorageReference profilePictureReference = storageReference.child("profile_pictures/"+currentUserID+".jpg");
                                                    // Getting image as bytes array
                                                    Bitmap profileImageBitmap = null;
                                                    try {
                                                        profileImageBitmap = getCorrectlyOrientedImage(EditSignupActivity.this,selectedImageURI,binding.selectImageIW.getMaxWidth());
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                                    assert profileImageBitmap != null;
                                                    profileImageBitmap.compress(Bitmap.CompressFormat.JPEG,75,baos);
                                                    byte[] profilePictureInBytes = baos.toByteArray();
                                                    if(selectedImageURI!=null){
                                                        // Upload profile picture to firebase storage
                                                        profilePictureReference.putBytes(profilePictureInBytes)
                                                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                    @Override
                                                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                        Toast.makeText(EditSignupActivity.this,"Image upload successfull",Toast.LENGTH_SHORT).show();
                                                                        // If image upload successful then get it's download link
                                                                        profilePictureReference.getDownloadUrl()
                                                                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                            @Override
                                                                            public void onSuccess(Uri uri) {
                                                                                String profilePictureURL = uri.toString();
                                                                                Map<String,Object> userInfoMap = new HashMap<>();
                                                                                userInfoMap.put("FirstName",firstName_str);
                                                                                userInfoMap.put("LastName",lastName_str);
                                                                                userInfoMap.put("Email",emailAddress_str);
                                                                                userInfoMap.put("Birthday",birthDay_str);
                                                                                userInfoMap.put("Gender",gender_str);
                                                                                userInfoMap.put("ProfilePictureURL",profilePictureURL);
                                                                                Date currentDate = new Date(new Date().getTime());
                                                                                userInfoMap.put("profileCreateDate",currentDate);
                                                                                mFirebaseFireStore
                                                                                        .collection("user_info")
                                                                                        .document(currentUserID)
                                                                                        .set(userInfoMap)
                                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                            @Override
                                                                                            public void onSuccess(Void unused) {
                                                                                                Toast.makeText(EditSignupActivity.this, "User info successfully added into database!", Toast.LENGTH_SHORT).show();
                                                                                            }
                                                                                        })
                                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                                            @Override
                                                                                            public void onFailure(@NonNull Exception e) {
                                                                                                Toast.makeText(EditSignupActivity.this, "Error: " + e.getMessage() , Toast.LENGTH_SHORT).show();
                                                                                            }
                                                                                        });
                                                                            }
                                                                        })
                                                                                .addOnFailureListener(new OnFailureListener() {
                                                                                    @Override
                                                                                    public void onFailure(@NonNull Exception e) {
                                                                                        Toast.makeText(EditSignupActivity.this, "Couldn't get image download url. - " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                                    }
                                                                                });
                                                                    }
                                                                })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Toast.makeText(EditSignupActivity.this, "Something went wrong! : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }

                                                }
                                            }
                                            else {
                                                Toast.makeText(EditSignupActivity.this, "Something went wrong when registration!", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                    }
                }
            });
        }
        else {
            binding.signUpTW.setText("Edit Profile");
            binding.signupBTN.setVisibility(View.GONE);
            binding.changePasswordBTN.setVisibility(View.VISIBLE);
            binding.saveChangesBTN.setVisibility(View.VISIBLE);
            binding.passwordLinearLayout.setVisibility(View.GONE);
            if(currentUser!=null){
                getProfileInformation();
            }
            else{
                Toast.makeText(this, "Couldn't get user information!", Toast.LENGTH_SHORT).show();
            }
            binding.saveChangesBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(checkInputValidations(processType)){
                        profileInfoUpdate();
                        profileImageUpdate();
                        updateEmail(emailAddress_str);
                    }
                }
            });
            binding.changePasswordBTN.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent changePasswordIntent = new Intent(EditSignupActivity.this,ChangePasswordActivity.class);
                    startActivity(changePasswordIntent);
                }
            });
        }


    }

    // Password and Email Validation for signup process
    public static boolean isValidPassword(final String password) {
        Pattern pattern;
        Matcher matcher;
        final String PASSWORD_PATTERN = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[.#?!@$%^&*-]).{8,}$";
        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);
        return matcher.matches();
    }
    public static boolean isValidEmail(CharSequence target) {
        return (!TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches());
    }

    // Checking and requesting gallery permission
    private boolean checkPermissions() {
        if (ContextCompat.checkSelfPermission(EditSignupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(EditSignupActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder permissionDialog = new AlertDialog.Builder(EditSignupActivity.this);
                permissionDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requestPermissions();
                    }
                });
                permissionDialog.setTitle("Permission Needed!");
                permissionDialog.setMessage("This app uses gallery permissions to pick image from gallery");
                permissionDialog.setCancelable(true);
            } else {
                requestPermissions();
            }
        }
        return false;
    }
    private void requestPermissions() {
        String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(EditSignupActivity.this, permissions, GALLERY_PERMISSION_REQUEST_CODE);
    }

    // Start an intent to select image from gallery
    public void pickImageFromGallery() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, GALLERY_PICK_IMAGE);
    }

    // Fixing the rotation problem, if image rotation is different than selected image from gallery
    public static int getOrientation(Context context, Uri photoUri) {

        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION}, null, null, null);

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

    // What will happen after image is selected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GALLERY_PICK_IMAGE) {
                selectedImageURI = data.getData();
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageURI);
                    binding.selectImageIW.setImageBitmap(getCorrectlyOrientedImage(EditSignupActivity.this, selectedImageURI, binding.selectImageIW.getMaxWidth()));
                } catch (IOException e) {
                    Log.i("TAG", "Some exception " + e);
                }
            }
        }
    }

    // Char to string gender transformation
    private String decodeUserGender(char gender_character)
    {
        if(gender_character == 'M'){
            return "Male";
        }
        else if(gender_character == 'F'){
            return "Female";
        }
        else{
            return "Other";
        }
    }

    // String to char gender transformation
    private char encodeUserGender(String gender_str){
        if(gender_str.equals("Male")){
            gender_chr = 'M';
        }
        else if(gender_str.equals("Female")){
            gender_chr = 'F';
        }
        else{
            gender_chr = 'O';
        }
        return gender_chr;
    }

    // Fill edit profile activity with profile information
    private void getProfileInformation()
    {
        String currentUserID = mAuth.getCurrentUser().getUid();
        StorageReference storageReference = mFirebaseStorage.getReference();
        StorageReference imageReference = storageReference.child("profile_pictures/"+currentUserID+".jpg");
        imageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                //Picasso.get().load(uri).into(binding.selectImageIW);
                Picasso.get().load(uri).placeholder(R.drawable.progress_animation).into(binding.selectImageIW);
            }
        });
        DocumentReference docRef = mFirebaseFireStore
                .collection("user_info")
                .document(currentUserID);
        System.out.println(currentUserID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                DocumentSnapshot document = task.getResult();
                if(document.exists()){
                    binding.firstNameET.setText((String)document.get("FirstName"));
                    binding.lastNameET.setText((String)document.get("LastName"));
                    binding.emailAddressET.setText((String)document.get("Email"));
                    if(document.get("Gender").equals("Male")){
                        genderSpinner.setSelection(1);
                    }
                    else if(document.get("Gender").equals("Female")){
                        genderSpinner.setSelection(2);
                    }
                    else{
                        genderSpinner.setSelection(3);
                    }
                    String birthday_str = (String)document.get("Birthday");
                    String[] dateParts = birthday_str.split("\\.");
                    birthday_day = Integer.parseInt(dateParts[0]);
                    birthday_month = Integer.parseInt(dateParts[1]);
                    birthday_year = Integer.parseInt(dateParts[2]);
                    binding.selectBirthdayBTN.setText("Birthday Date: " + birthday_day + "." + birthday_month + "." + birthday_year);
                    binding.selectBirthdayBTN.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mDatePicker = new DatePickerDialog(EditSignupActivity.this, new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                                    birthday_year = i;
                                    birthday_month = i1;
                                    birthday_day = i2;
                                    binding.selectBirthdayBTN.setText("Selected Date: " + birthday_day + "." + birthday_month + "." + birthday_year);
                                }
                            },birthday_year,birthday_month,birthday_day);
                            Calendar cal = Calendar.getInstance();
                            long currentTime = cal.getTimeInMillis();
                            mDatePicker.getDatePicker().setMaxDate(currentTime);
                            cal.add(Calendar.YEAR,-82);
                            long minDate = cal.getTimeInMillis();
                            mDatePicker.getDatePicker().setMinDate(minDate);
                            mDatePicker.show();
                        }
                    });
                }
                else{
                    System.out.println("There is no user with this id!");
                }
            }
        });
    }

    // Check input validation depends on processType
    private boolean checkInputValidations(String processType)
    {
        firstName_str = binding.firstNameET.getText().toString();
        lastName_str = binding.lastNameET.getText().toString();
        emailAddress_str = binding.emailAddressET.getText().toString();
        password_str = binding.passwordET.getText().toString();
        if(processType.equals("signup")){
            if (firstName_str.isEmpty() || lastName_str.isEmpty() || emailAddress_str.isEmpty() || password_str.isEmpty()) {
                Toast.makeText(EditSignupActivity.this, "Please fill blank area(s)!", Toast.LENGTH_SHORT).show();
                return false;
            }
            else {
                if (isValidEmail(emailAddress_str) && isValidPassword(password_str) && gender_chr != 'X' && isOlderThanEighteenYearsOld())
                {
                    return true;
                }
                else{
                    if (!isValidPassword(password_str)) {
                        Toast.makeText(EditSignupActivity.this, "Password is not strong enough!", Toast.LENGTH_SHORT).show();
                    } else if (!isValidEmail(emailAddress_str)) {
                        Toast.makeText(EditSignupActivity.this, "E-mail address format is not valid!", Toast.LENGTH_SHORT).show();
                    } else if (!isOlderThanEighteenYearsOld()) {
                        Toast.makeText(EditSignupActivity.this, "You have to more than 18 years old to sign up!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditSignupActivity.this, "Gender not selected please select the gender!", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            }
        }
        else{
            if (firstName_str.isEmpty() || lastName_str.isEmpty() || emailAddress_str.isEmpty()) {
                Toast.makeText(EditSignupActivity.this, "Please fill blank area(s)!", Toast.LENGTH_SHORT).show();
                return false;
            }
            else {
                if (isValidEmail(emailAddress_str) && gender_chr != 'X' && isOlderThanEighteenYearsOld())
                {
                    return true;
                }
                else{
                    if (!isValidEmail(emailAddress_str)) {
                        Toast.makeText(EditSignupActivity.this, "E-mail address format is not valid!", Toast.LENGTH_SHORT).show();
                    } else if (!isOlderThanEighteenYearsOld()) {
                        Toast.makeText(EditSignupActivity.this, "You have to more than 18 years old!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(EditSignupActivity.this, "Gender not selected please select the gender!", Toast.LENGTH_SHORT).show();
                    }
                    return false;
                }
            }
        }


    }

    // Checks if user's age older than eighteen
    private boolean isOlderThanEighteenYearsOld()
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate today = LocalDate.now();
            LocalDate birthday = LocalDate.of(birthday_year,birthday_month,birthday_day);
            Period period = Period.between(birthday,today);
            if(period.getYears()>=18){
                return true;
            }
            else{
                return false;
            }
        }
        else{
         return false;
        }
    }

    // Update user's profile picture
    private void profileImageUpdate()
    {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userUID = currentUser.getUid();
        StorageReference storageReference = mFirebaseStorage.getReference();
        StorageReference profilePictureRef = storageReference.child("profile_pictures/"+userUID+".jpg");
        // Getting image as bytes array
        Bitmap profileImageBitmap = null;
        try {
            profileImageBitmap = getCorrectlyOrientedImage(EditSignupActivity.this,selectedImageURI,binding.selectImageIW.getMaxWidth());
        } catch (IOException e) {
            e.printStackTrace();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        assert profileImageBitmap != null;
        profileImageBitmap.compress(Bitmap.CompressFormat.JPEG,75,baos);
        byte[] profilePictureInBytes = baos.toByteArray();
        if(selectedImageURI!=null){
            profilePictureRef.putBytes(profilePictureInBytes)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(EditSignupActivity.this, "New image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            profilePictureRef.getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            String profilePicturedURL = uri.toString();
                                            WriteBatch batch = mFirebaseFireStore.batch();
                                            DocumentReference docRef = mFirebaseFireStore
                                                    .collection("user_info")
                                                    .document(userUID);
                                            batch.update(docRef,"ProfilePictureURL",profilePicturedURL);
                                            batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void unused) {
                                                    Toast.makeText(EditSignupActivity.this, "Profile picture successfully updated!", Toast.LENGTH_SHORT).show();
                                                }
                                            })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Toast.makeText(EditSignupActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(EditSignupActivity.this, "Error occured when image uploading : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    // Update user's info on Firebase firestore
    private void profileInfoUpdate()
    {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String uid = currentUser.getUid();
        WriteBatch batch = mFirebaseFireStore.batch();
        DocumentReference docRef = mFirebaseFireStore
                .collection("user_info")
                .document(uid);
        batch.update(docRef,"FirstName",firstName_str);
        batch.update(docRef,"LastName",lastName_str);
        batch.update(docRef,"Email",emailAddress_str);
        batch.update(docRef,"Gender",decodeUserGender(gender_chr));
        batch.update(docRef,"Birthday",birthday_day+"."+birthday_month+"."+birthday_year);
        batch.commit()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Toast.makeText(EditSignupActivity.this,"User profile successfully updated!",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(EditSignupActivity.this,"Error occured: " + e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmail(String newEmail){
        FirebaseUser currentUser = mAuth.getCurrentUser();
        DocumentReference docRef = mFirebaseFireStore
                .collection("user_info")
                .document(currentUser.getUid());
        WriteBatch batch = mFirebaseFireStore.batch();
        SharedPreferences sharedPreferences = this.getSharedPreferences("sharedPref",Context.MODE_PRIVATE);
        String loginEmail = sharedPreferences.getString("loginEmail","");
        String loginPass = sharedPreferences.getString("loginPass","");
        System.out.println(loginEmail + " " + loginPass);
        FirebaseUser user = mAuth.getCurrentUser();
        AuthCredential credential = EmailAuthProvider
                .getCredential(loginEmail,loginPass);
        user.reauthenticate(credential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        if(!loginEmail.equals(newEmail)){
                            AlertDialog.Builder updateEmailDialog = new AlertDialog.Builder(EditSignupActivity.this);
                            updateEmailDialog.setTitle("Are you sure?");
                            updateEmailDialog.setMessage("If you update your email address you must re-sign in.");
                            updateEmailDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    user.updateEmail(newEmail)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if(task.isSuccessful()){
                                                        task.addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void unused) {
                                                                batch.update(docRef,"Email",newEmail).commit()
                                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                            @Override
                                                                            public void onSuccess(Void unused) {
                                                                                mAuth.signOut();
                                                                                finish();
                                                                                Intent loginIntent = new Intent(EditSignupActivity.this,MainActivity.class);
                                                                                startActivity(loginIntent);
                                                                                Toast.makeText(EditSignupActivity.this, "Email successfully updated!", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        })
                                                                        .addOnFailureListener(new OnFailureListener() {
                                                                            @Override
                                                                            public void onFailure(@NonNull Exception e) {
                                                                                Toast.makeText(EditSignupActivity.this, "Updating of email address is unsuccessful!", Toast.LENGTH_SHORT).show();
                                                                            }
                                                                        });
                                                            }
                                                        })
                                                                .addOnFailureListener(new OnFailureListener() {
                                                                    @Override
                                                                    public void onFailure(@NonNull Exception e) {
                                                                        Toast.makeText(EditSignupActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    }
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Toast.makeText(EditSignupActivity.this, "Email update not successful!", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            }).setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(EditSignupActivity.this, "E-mail update canceled!", Toast.LENGTH_SHORT).show();
                                }
                            }).create().show();

                        }
                    }
                });
    }



}


