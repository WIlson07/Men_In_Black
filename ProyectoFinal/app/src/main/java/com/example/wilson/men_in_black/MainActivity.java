package com.example.wilson.men_in_black;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.picasso.Picasso;

import org.openalpr.OpenALPR;
import org.openalpr.model.Result;
import org.openalpr.model.Results;
import org.openalpr.model.ResultsError;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE = 100;
    private static final int STORAGE=1;
    private int PICK_IMAGE_REQUEST = 1;
    static final String RUNTIME_DATA_DIR_ASSET = "runtime_data";
    static final String ANDROID_DATA_DIR = "/data/data/com.example.wilson.men_in_black";
    static final String OPENALPR_CONF_FILE = "openalpr.conf";
    private static File destination;
    private TextView resultTextView;
    private ImageView imageView;
    private Button button;
    final String openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + RUNTIME_DATA_DIR_ASSET + File.separatorChar + OPENALPR_CONF_FILE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //this.getApplicationInfo().dataDir;


        button = findViewById(R.id.button);
        resultTextView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView);

        resultTextView.setText("Press the button to start a request");




        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();


                Log.wtf("prueba", ANDROID_DATA_DIR);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE && resultCode == Activity.RESULT_OK){
            final ProgressDialog progress = ProgressDialog.show(this, "Loading",
                    "Parsing result...", true);

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 10;

                    // Picasso requires permission.WRITE_EXTERNAL_STORAGE
                    Picasso.get().load(destination).fit().centerCrop().into(imageView);
                    resultTextView.setText("Processing");

                    String result = OpenALPR.Factory.create(MainActivity.this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("us", "", destination.getAbsolutePath(), openAlprConfFile, 2);
                    Log.wtf("abspath", destination.getAbsolutePath());


                    Log.d("OPEN ALPR", result);

                    try{
                        final Results results = new Gson().fromJson(result, Results.class);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (results == null || results.getResults() == null ||
                                        results.getResults().size() == 0){
                                    Toast.makeText(MainActivity.this, "It was not possible to detect the license plate.", Toast.LENGTH_LONG).show();
                                    resultTextView.setText("It was not possible to detect a licence plate");
                                } else {

                                    resultTextView.setText("Plate: " + results.getResults().get(0).getPlate()
                                            // Trim confidence to two decimal places
                                            + " Confidence: " + String.format("%.2f", results.getResults().get(0).getConfidence()) + "%"
                                            // Convert processing time to seconds and trim to two decimal places
                                            + " Processing time: "+ String.format("%.2f", ((results.getProcessingTimeMs() + 1000.0) % 60)) + " seconds");
                                }
                            }
                        });
                    } catch (JsonSyntaxException exception){
                        final ResultsError resultsError = new Gson().fromJson(result, ResultsError.class);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                resultTextView.setText(resultsError.getMsg());
                            }
                        });
                    }

                    progress.dismiss();
                }
            });
        }


        else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null){
            final ProgressDialog progress = ProgressDialog.show(this, "Loading",
                    "Parsing result...", true);

            Uri uri = data.getData();



            try {
                Bitmap bmImg = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);

                String path = null;
                try {
                    path = getRealPath.getPath(MainActivity.this, uri);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                Picasso.get().load(uri).fit().centerCrop().into(imageView);

                //imageView.setImageBitmap(bmImg);

                String result = OpenALPR.Factory.create(MainActivity.this, ANDROID_DATA_DIR).recognizeWithCountryRegionNConfig("us", "", path, openAlprConfFile, 2);
                Log.d("OPEN ALPR", result);

                try{
                    final Results results = new Gson().fromJson(result, Results.class);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (results == null || results.getResults() == null ||
                                    results.getResults().size() == 0){
                                Toast.makeText(MainActivity.this, "It was not possible to detect the license plate.", Toast.LENGTH_LONG).show();
                                resultTextView.setText("It was not possible to detect a licence plate");
                            } else {
                                resultTextView.setText("Plate: " + results.getResults().get(0).getPlate()
                                        // Trim confidence to two decimal places
                                        + " Confidence: " + String.format("%.2f", results.getResults().get(0).getConfidence()) + "%"
                                        // Convert processing time to seconds and trim to two decimal places
                                        + " Processing time: "+ String.format("%.2f", ((results.getProcessingTimeMs() + 1000.0) % 60)) + " seconds");
                            }
                        }
                    });
                } catch (JsonSyntaxException exception){
                    final ResultsError resultsError = new Gson().fromJson(result, ResultsError.class);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            resultTextView.setText(resultsError.getMsg());
                        }
                    });
                }

                progress.dismiss();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }




    }

    private void checkPermission() {
        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()){
            Toast.makeText(this, "Storage access needed to manage the picture.", Toast.LENGTH_LONG).show();
            String[] params = permissions.toArray(new String[permissions.size()]);
            ActivityCompat.requestPermissions(this, params, STORAGE);
        } else {
            Log.wtf("prueba2", "tomara captura");
            takePicture();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case STORAGE:{
                Map<String, Integer> perms = new HashMap<>();
                //Initial
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);

                //Fill with results
                for(int i = 0; i < permissions.length; i++) {
                    perms.put(permissions[i], grantResults[i]);
                }

                    //Check for WRITE_EXTERNAL_STORAGE
                Boolean storage = perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                 if (storage){
                     //permission was granted
                     takePicture();

                }else {
                     //Permission denied
                     Toast.makeText(this, "Storage permission is needed", Toast.LENGTH_LONG).show();
                 }
            }
            default:
                break;
        }
    }

    public String dateToString(Date date, String format){
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.getDefault());

        return df.format(date);
    }

    private void takePicture() {
        //Use a folder to store all results
        File folder = new File(Environment.getExternalStorageDirectory().getPath());
        if (!folder.exists()){
            folder.mkdir();
        }

        //Generate the path for the next photo
        String name = dateToString(new Date(), "yyyy-MM-dd-hh-mm-ss");
        destination = new File(folder, name + ".jpg");

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), "com.example.wilson.men_in_black.fileprovider", destination));//(destination));
        startActivityForResult(intent, REQUEST_IMAGE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (destination != null){
            Picasso.get().load(destination).fit().centerCrop().into(imageView);
        }
    }

    public class GenericFileProvider extends FileProvider{}

    public void openGallery(View v){
        Intent inGallery = new Intent();
        inGallery.setType("image/*");
        inGallery.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(inGallery,"Select Picture"), PICK_IMAGE_REQUEST);
    }


}
