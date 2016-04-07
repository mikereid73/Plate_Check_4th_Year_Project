package c00112726.itcarlow.ie.finalyearproject.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;
import c00112726.itcarlow.ie.finalyearproject.tasks.LoginTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallbackJSON;

public class LoginActivity extends AppCompatActivity implements TaskCallbackJSON {

    private static final String TAG = "LoginActivity";

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 10;
    private static final int MAX_PASSWORD_LENGTH = 10;

    private static final String KEY_SUCCESS = "success";

    private EditText mUsernameText;
    private EditText mPasswordText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mUsernameText =(EditText)findViewById(R.id.input_email);
        mPasswordText =(EditText)findViewById(R.id.input_password);

        Button loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = String.valueOf(mUsernameText.getText()).trim();
                String password = String.valueOf(mPasswordText.getText()).trim();
                attemptLogin(username, password);
            }
        });
    }

    /**
     * Check username and password supplied match expected lengths
     * @param username username
     * @param password password
     * @return whether username and password lengths are valid
     */
    protected boolean credentialValid(String username, String password) {
        final int uLength = username.length();
        final int pLength = password.length();
        return (uLength >= MIN_USERNAME_LENGTH && uLength <= MAX_USERNAME_LENGTH &&
                pLength >= MIN_PASSWORD_LENGTH && pLength <= MAX_PASSWORD_LENGTH);
    }

    /**
     * Attempt to log in using supplied username and password.
     * First a check is performed to ensure credentials are in correct format.
     * Second a check is performed to ensure there is a network connection.
     * Finally if everything is ok, start task.
     * @param username username
     * @param password password
     */
    protected void attemptLogin(String username, String password) {
        if(!credentialValid(username, password)) {
            String message = getString(R.string.request_credentials_1);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }

        if(!Util.isNetworkAvailable(this)) {
            String message = getString(R.string.no_network_1);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }

        LoginTask lt = new LoginTask(this);
        lt.execute(username, password);
    }

    /**
     * Called when LoginTask thread completes
     */
    @Override
    public void onTaskComplete(JSONObject json) {
        if(json == null) {
            String message = getString(R.string.null_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }

        try {
            String response = json.getString(KEY_SUCCESS);
            if(Boolean.parseBoolean(response)) {
                String message = getString(R.string.login_success);
                Util.showToast(this, message, Toast.LENGTH_SHORT);
                onLoginSuccess();
            }
            else {
                String message = getString(R.string.login_fail);
                Util.showToast(this, message, Toast.LENGTH_SHORT);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            String message = getString(R.string.bad_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
        }
    }

    private void onLoginSuccess() {
        SharedPreferences sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String username = String.valueOf(mUsernameText.getText()).trim();
        editor.putString("username", username );
        editor.apply(); // .comit();

        Intent intent = new Intent(this, MainMenuActivity.class);
        startActivity(intent);
    }
}
