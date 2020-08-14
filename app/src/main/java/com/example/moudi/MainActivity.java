package com.example.moudi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    FirebaseAutoMLRemoteModel remoteModel;
    FirebaseVisionImageLabeler labeler;
    FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder optionBuilder;
    FirebaseModelDownloadConditions conditions;
    FirebaseVisionImage image;
    TextView tv_result;
    Button btn_diagnosis;
    ImageView imageView;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_result = findViewById(R.id.tv_hasil);
        imageView = findViewById(R.id.view_picture);
        btn_diagnosis = findViewById(R.id.btn_gambar);
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Harap Tunggu...");
        progressDialog.setCanceledOnTouchOutside(false);
        
        btn_diagnosis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fromRemoteModel();
            }
        });
    }

    private void fromRemoteModel() {
        progressDialog.show();
        remoteModel = new FirebaseAutoMLRemoteModel.Builder("Pneumonia_202072535235").build();
        conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();

        FirebaseModelManager.getInstance().download(remoteModel, conditions)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        CropImage.activity().start(MainActivity.this);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this,"err"+e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){
                if (result != null){
                    Uri uri = result.getUri();
                    imageView.setImageURI(uri);

                    tv_result.setText("");
                    setLabelerFromRemoteLabel(uri);
                }else {
                    progressDialog.cancel();
                }
            }else {
                progressDialog.cancel();
            }
        }
    }

    private void setLabelerFromRemoteLabel(final Uri uri) {
    FirebaseModelManager.getInstance().isModelDownloaded(remoteModel)
            .addOnCompleteListener(new OnCompleteListener<Boolean>() {
                @Override
                public void onComplete(@NonNull Task<Boolean> task) {
                    if (task.isComplete()){
                        optionBuilder = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel);
                        FirebaseVisionOnDeviceAutoMLImageLabelerOptions options = optionBuilder
                                .setConfidenceThreshold(0.0f)
                                .build();
                        try {
                            labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
                            image = FirebaseVisionImage.fromFilePath(MainActivity.this, uri);
                            processImageLabeler(labeler, image);
                            
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, "Ml exeception", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Not downloaded", Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
            Toast.makeText(MainActivity.this, "err"+e, Toast.LENGTH_SHORT).show();     
        }
    });

    }

    private void processImageLabeler(FirebaseVisionImageLabeler labeler, FirebaseVisionImage image) {
        labeler.processImage(image).addOnCompleteListener(new OnCompleteListener<List<FirebaseVisionImageLabel>>() {

            @Override
            public void onComplete(@NonNull Task<List<FirebaseVisionImageLabel>> task) {
                progressDialog.cancel();
                for (FirebaseVisionImageLabel label : task.getResult()){
                    String eachlabel = label.getText().toUpperCase();
                    float confidence = label.getConfidence();
                    tv_result.append(eachlabel + " : " + ("" + confidence * 100).subSequence(0, 4) + "%" + "\n\n");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }


}