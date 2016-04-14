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

import org.json.JSONObject;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;
import c00112726.itcarlow.ie.finalyearproject.tasks.ChangePasswordTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallbackJSON;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 05/04/2016
 */
public class EditPasswordActivity extends AppCompatActivity implements TaskCallbackJSON {

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
            String message = getString(R.string.empty_field);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return false;
        }
        else if(currentPassword.length() < 3 || newPassword.length() < 3 || confirmPassword.length() < 3) {
            String message = getString(R.string.password_short);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return false;
        }
        else if(!newPassword.equals(confirmPassword)) {
            String message = getString(R.string.password_no_match);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return false;
        }
        else if(newPassword.equals(currentPassword)) {
            String message = getString(R.string.password_same);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
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

    @Override
    public void onTaskComplete(JSONObject json) {
        if(json == null) {
            String message = getString(R.string.connect_failed);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }
        try {
            String response = json.getString("success");
            if(Boolean.parseBoolean(response)) {
                String message = getString(R.string.password_change_success);
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
            }
            else {
                String reason = (json.getString("reason")) + "\n";
                String message = getString(R.string.password_change_fail);
                Toast.makeText(this, message + reason, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            String message = getString(R.string.bad_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            Log.e(TAG, e.getMessage());
        }
    }
}
