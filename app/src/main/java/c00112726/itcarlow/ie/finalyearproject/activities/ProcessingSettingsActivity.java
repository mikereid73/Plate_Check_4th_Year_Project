package c00112726.itcarlow.ie.finalyearproject.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 08/04/2016
 */
public class ProcessingSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processing_settings);
        setTitle("Pre-processing Settings");
    }

    @Override
    protected void onResume() {
        super.onResume();

        String message = "Feature not fully supported";
        Util.showToast(this, message, Toast.LENGTH_LONG);
    }
}
