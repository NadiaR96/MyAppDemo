package com.rodgers.myappdemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DemoScreen extends AppCompatActivity {

    private int INTERNET_PERMISSION_CODE = 1;
    private RadioGroup radioMethodGroup;
    private RadioButton radioMethodButton;
    private Button btnDisplay;
    public TextView tv_result1,tv_result2, startLoc, endLoc;
    public String URL_STRING, origin, destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo_screen);

        //get details for current user - if none found go back to login
        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        //user found - display details on screen to confirm correct user
        TextView email = (TextView) findViewById(R.id.email);
        TextView displayname = (TextView) findViewById(R.id.displayname);
        email.setText(currentUser.getEmail());
        displayname.setText(currentUser.getDisplayName());
        //wait for information to be entered by user and button click
        addListenerOnButton();
    }

    public void addListenerOnButton() {

        radioMethodGroup = (RadioGroup) findViewById(R.id.radioMethod);
        btnDisplay = (Button) findViewById(R.id.btnDisplay);

        btnDisplay.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //following line of code will set button to invisible and limit to single call
                //for demonstration purposes all multiple calls to show different functionality
                //btnDisplay.setVisibility(View.INVISIBLE);

                // get selected radio button from radioGroup - to give mode of travel
                int selectedId = radioMethodGroup.getCheckedRadioButtonId();

                // find the radiobutton by returned id
                radioMethodButton = (RadioButton) findViewById(selectedId);
                String mode;
                mode = radioMethodButton.getText().toString();

                // build example call based on input locatioons and mode of travel
                startLoc = (TextView) findViewById(R.id.editText_From);
                endLoc = (TextView) findViewById(R.id.editTo);
                origin = startLoc.getText().toString().trim();
                destination = endLoc.getText().toString().trim();

                //TODO - for real app - split into different parts and combine
                //TODO - for real app - will need to include arrival time for transit

                URL_STRING = "https://maps.googleapis.com/maps/api/distancematrix/json?units=imperial&origins="+origin+"&destinations="+destination+"&mode="+mode+"&key=AIzaSyBAfAkbPGDVtnObUy7WQs60DykkqCoMW3g";

                //NB INTERNET is SOFT permission so should be automatically given so this check is not really necessary
                //Included here as permission for eg users phone location (ACCESS_FINE_LOCATION) may be necessary for call in real app
                if (ContextCompat.checkSelfPermission(DemoScreen.this,
                        Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
                   //Permission to use internet for call ok

                    //executing Async task class to create HTTP connection
                    new FetchInfo().execute();
                } else {
                    //try to get permission
                    requestInternetPermission();
                }
            }

        });

    }
    //This code include for ease of transitioning to real app.
    //If permission not already granted then this code asks for permission and then explains why permission required
    //Process cancelled if permission not given
    private void requestInternetPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.INTERNET)) {

            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed to get time of travel")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(DemoScreen.this,
                                    new String[] {Manifest.permission.INTERNET}, INTERNET_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.INTERNET}, INTERNET_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == INTERNET_PERMISSION_CODE)  {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new FetchInfo().execute();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
    //end of request for permission

    /* Start Async task class to get json response by making HTTP call
     * Async task class is used as you cannot create a network connection on main thread
     */
    /*adopted from various coding examples on GitHub and Stackoverflow */
    public class FetchInfo extends AsyncTask<Void, Void, Void> {

        ProgressDialog progressDialog;
        String response;

        @Override
        protected void onPreExecute() {
            //set progress dialog going
            super.onPreExecute();
            progressDialog = new ProgressDialog(DemoScreen.this);
            progressDialog.setCancelable(false);
            progressDialog.setMessage("Please wait. Fetching data..");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setProgress(0);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //cancel progress dialog
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            //Toast included for testing purposes and commented out for demo
            //Toast.makeText(getApplicationContext(),
            //        "Connection successful.", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String response = "";
            HttpURLConnection conn;
            StringBuilder jsonResults = new StringBuilder();

            try {
                //setting URL to connect with = URL previously constructed
                URL url = new URL(URL_STRING);
                //creating connection
                conn = (HttpURLConnection) url.openConnection();

                //converting response into String
                InputStreamReader in = new InputStreamReader(conn.getInputStream());
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    jsonResults.append(buff, 0, read);
                }
                response = jsonResults.toString();
                //parse json Log.d statements to aide with development
                Log.d("JSON", response);
                JSONObject root = new JSONObject(response);
                JSONArray array_rows = root.getJSONArray("rows");
                Log.d("JSON", "array_rows:" + array_rows);
                JSONObject object_rows = array_rows.getJSONObject(0);
                Log.d("JSON", "object_rows:" + object_rows);
                JSONArray array_elements = object_rows.getJSONArray("elements");
                Log.d("JSON", "array_elements:" + array_elements);
                JSONObject object_elements = array_elements.getJSONObject(0);
                Log.d("JSON", "object_elements:" + object_elements);

                JSONObject object_duration = object_elements.getJSONObject("duration");
                JSONObject object_distance = object_elements.getJSONObject("distance");

                Log.d("JSON", "object_duration:" + object_duration);

                /*response includes value and text - for purposes of demonstration we
                 * are displaying text.  In real app would convert values received to ensure accuracy
                 */
                String duration = object_duration.getString("text");
                String distance = object_distance.getString("text");

                if (duration !=null) {
                    String duration1 = "Duration= "+ duration;
                    String distance1 = "Distance= " + distance;
                    Log.d("JSON", duration1+" "+distance1);
                    setResults(duration1, distance1);
                }
            } catch (Exception e) {
                setResults ("No result found","No result found");
                e.printStackTrace();
            }
            return null;
        }

    }
    public void setResults (String dur, String dis) {
        final String dur1 = dur;
        final String dis1 = dis;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_result1= (TextView) findViewById(R.id.textView_duration);
                tv_result2=(TextView) findViewById(R.id.textView_distance);
                tv_result1.setText(dur1);
                tv_result2.setText(dis1);
            }
        });

    }

    /* Code from FirebaseUI Auth to signout of app */
    public void signOut(View view) {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(
                                    DemoScreen.this,
                                    MainActivity.class));
                            finish();
                        } else {
                            // Report error to user
                            Toast.makeText(getApplicationContext(),
                                    "Error logging out - please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    /* Code from FirebaseUI Auth to delete */
    public void deleteAccount(View view) {
        AuthUI.getInstance()
                .delete(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            startActivity(new Intent(DemoScreen.this,
                                    MainActivity.class));
                            finish();
                        } else {
                            // Notify user of error
                            //Addition of simple Toast to notify user.
                            //TODO check for reason for error and expand explanation
                            Toast.makeText(getApplicationContext(),
                                    "Error deleting user - please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
