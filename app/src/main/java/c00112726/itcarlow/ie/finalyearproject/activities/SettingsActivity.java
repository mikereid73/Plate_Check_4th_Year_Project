package c00112726.itcarlow.ie.finalyearproject.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button advanceSettings = (Button)findViewById(R.id.btn_advanced_settings);
        advanceSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = getString(R.string.not_implemented);
                Util.showToast(SettingsActivity.this, message, Toast.LENGTH_SHORT);
            }
        });

        Button changePassword = (Button)findViewById(R.id.btn_change_password);
        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, EditPasswordActivity.class);
                startActivity(intent);
            }
        });
    }
}

