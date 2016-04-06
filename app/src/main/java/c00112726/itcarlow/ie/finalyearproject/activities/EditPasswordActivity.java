package c00112726.itcarlow.ie.finalyearproject.activities;

import android.content.Context;
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
import c00112726.itcarlow.ie.finalyearproject.tasks.ChangePasswordTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.TaskCallback;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.ChangePasswordCallback;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 05/04/2016
 */
public class EditPasswordActivity extends AppCompatActivity implements ChangePasswordCallback {

    private static final String TAG = "EditPasswordActivity";

    protected EditText mCurrentPassword;
    protected EditText mNewPassword;
    protected EditText mConfirmNewPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_password);

        mCurrentPassword = (EditText)findViewById(R.id.input_current_password);
        mNewPassword = (EditText)findViewById(R.id.input_new_password);
        mConfirmNewPassword = (EditText)findViewById(R.id.input_confirm_new_password);

        Button submitBtn = (Button)findViewById(R.id.btn_submit);
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkUniquePassword()) {
                    updateUserPasswords();
                }
            }
        });

        Button cancelBtn = (Button)findViewById(R.id.btn_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditPasswordActivity.this.finish();
            }
        });
    }

    private boolean checkUniquePassword() {
        String currentPassword = String.valueOf(mCurrentPassword.getText()).trim();
        String newPassword = String.valueOf(mNewPassword.getText()).trim();
        String confirmPassword = String.valueOf(mConfirmNewPassword.getText()).trim();

        if(currentPassword.equals("") || newPassword.equals("") || confirmPassword.equals("")) {
            showToast("One or more fields are empty");
            return false;
        }
        else if(currentPassword.length() < 3 || newPassword.length() < 3 || confirmPassword.length() < 3) {
            showToast("Password too short");
            return false;
        }
        else if(!newPassword.equals(confirmPassword)) {
            showToast("New passwords don't match");
            return false;
        }
        else if(newPassword.equals(currentPassword)) {
            showToast("New password cannot be the same as the current password");
            return false;
        }
        else {
            return true;
        }
    }

    private void updateUserPasswords() {
        if(Util.isNetworkAvailable(this)) {
            SharedPreferences sharedPreferences = getSharedPreferences("login", Context.MODE_PRIVATE);
            String username = sharedPreferences.getString("username", "admin");
            String currentPassword = String.valueOf(mCurrentPassword.getText()).trim();
            String newPassword = String.valueOf(mNewPassword.getText()).trim();

            ChangePasswordTask changePasswordTask = new ChangePasswordTask(this);
            changePasswordTask.execute(username, currentPassword, newPassword);
        }
        else {
            String message = getString(R.string.no_network_1);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
        }
    }

    private void showToast(String message) {
        Toast.makeText(EditPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onChangePasswordComplete(JSONObject result) {
        try {
            String response = (result != null ? result.getString("success") : "false");
            if(Boolean.parseBoolean(response)) {
                Toast.makeText(this, "Change successful", Toast.LENGTH_SHORT).show();
                finish();
            }
            else {
                String reason = (result != null ? result.getString("reason") : "unknown");
                Toast.makeText(this, "Change failed\n" + reason, Toast.LENGTH_SHORT).show();
            }
        } catch (JSONException e) {
            Log.wtf(TAG, e.getMessage());
            Toast.makeText(this, "Fatal Error", Toast.LENGTH_SHORT).show();
        }
    }
}
