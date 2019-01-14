package com.rodgers.myappdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.firebase.ui.auth.AuthUI;

import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private static final int REQUEST_CODE = 101;
    String TAG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            //user already signed in (not logged out after last session - go to application
            startActivity(new Intent(this, DemoScreen.class));
            finish();
        } else {
            // Welcome to app screen displayed
            //start login process when button clicked
            Button button = (Button) findViewById(R.id.Login_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    authenticateUser();
                }
            });
        }
    }

    // Next part of coding adapted from FireBaseUI Auth standard code on GitHub
    private List<AuthUI.IdpConfig> getProviderList() {

        //list of sign in methods given in standard code- only email considered here.
        // TODO For full app - Consider adding additional signin methods if time
        List<AuthUI.IdpConfig> providers = new ArrayList<>();

        providers.add(
                new AuthUI.IdpConfig.EmailBuilder().build());

        return providers;
    }

    private void authenticateUser() {
        //call to FirebaseUI to start sign in process
        // TODO for full app - consider use of Smartlock (set to true if required)
        // TODO for full app - consider Theme of Login - .setTheme(R.style.CustomTheme)
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(getProviderList())
                        .setIsSmartLockEnabled(false)
                        .build(),
                REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //return from sign in activity - check results

        super.onActivityResult(requestCode, resultCode, data);

        IdpResponse response = IdpResponse.fromResultIntent(data);

        if (requestCode == REQUEST_CODE) {

            if (resultCode == RESULT_OK) {
                //successful signin
                startActivity(new Intent(this, DemoScreen.class));
                return;
            }
        } else {
            if (response == null) {
                // User cancelled Sign-in
                // TODO check use of Snackbar to display errors
                //Snackbar.make(findViewById(R.id.), R.string.email_sent,
                //        Snackbar.LENGTH_SHORT)
                //        .show();
                //Snackbar.show(R.string.sign_in_cancelled);
                Log.e(TAG, "Sign-in cancelled: ", response.getError());
                return;
            }

            if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                // Device has no network connection
                Log.e(TAG, "No network: ", response.getError());
                return;
            }

            if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                // Unknown error occurred
                //showSnackbar(R.string.unknown_error);
                Log.e(TAG, "Sign-in error: ", response.getError());
                return;
            }
        }
    }
    // End of coding adapted from FireBaseUI Auth standard code on GitHub
}