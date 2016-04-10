package c00112726.itcarlow.ie.finalyearproject.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import c00112726.itcarlow.ie.finalyearproject.R;
import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;
import c00112726.itcarlow.ie.finalyearproject.misc.Util;
import c00112726.itcarlow.ie.finalyearproject.tasks.DatabaseConnectionTask;
import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallbackJSON;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 26/03/2016
 */
public class EditRegActivity extends AppCompatActivity implements TaskCallbackJSON {

    private static final String TAG = "EditRegActivity";

    private EditText mNumberPlateNumber;
    private EditText mEditYear;
    private EditText mEditCounty;
    private EditText mEditReg;

    private NumberPlate mNumberPlate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_reg);

        final File file = (File)getIntent().getSerializableExtra("image file");
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath(), bmOptions);

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setImageBitmap(image);

        mNumberPlate = (NumberPlate)getIntent().getSerializableExtra("number plate");

        mNumberPlateNumber = (EditText)findViewById(R.id.regNumber);

        mEditYear = (EditText)findViewById(R.id.year);
        mEditCounty = (EditText)findViewById(R.id.county);
        mEditReg = (EditText)findViewById(R.id.registration);

        mEditYear.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegDisplay();
            }
        });
        mEditCounty.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegDisplay();
            }
        });
        mEditReg.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateRegDisplay();
            }
        });

        mEditYear.setText(mNumberPlate.getYear());
        mEditCounty.setText(mNumberPlate.getCounty());
        mEditReg.setText(mNumberPlate.getReg());

        Button continueButton = (Button)findViewById(R.id.ok);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadToDatabase();
            }
        });
        Button cancelButton = (Button) findViewById(R.id.cancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    public void onTaskComplete(JSONObject json) {
        if(json == null) {
            String message = getString(R.string.null_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
            return;
        }
        String KEY_REGISTRATION = "registration";
        String KEY_INFRACTION = "infraction";
        try {
            String infraction = json.getString(KEY_INFRACTION);
            String registration = json.getString(KEY_REGISTRATION);
            if(Boolean.parseBoolean(infraction)) {
                String message = getString(R.string.infraction_occured);
                message += "\nRegistration: " + registration;
                Util.showToast(this, message, Toast.LENGTH_LONG);
            }
            else {
                String message = getString(R.string.no_infraction);
                message += "\nRegistration: " + registration;
                Util.showToast(this, message, Toast.LENGTH_LONG);
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
            String message = getString(R.string.bad_json);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
        }
        finish();
    }

    private void updateRegDisplay() {
        String year = String.valueOf(mEditYear.getText());
        String county = String.valueOf(mEditCounty.getText());
        String reg = String.valueOf(mEditReg.getText());
        String newText = year + "-" + county + "-" + reg;
        mNumberPlateNumber.setText(newText);
    }

    private void uploadToDatabase() {
        String year = String.valueOf(mEditYear.getText());
        String county = String.valueOf(mEditCounty.getText());
        String reg = String.valueOf(mEditReg.getText());

        mNumberPlate.setYear(year);
        mNumberPlate.setCounty(county);
        mNumberPlate.setReg(reg);

        if(Util.isNetworkAvailable(EditRegActivity.this)) {
            DatabaseConnectionTask dbt = new DatabaseConnectionTask(this);
            dbt.execute(mNumberPlate);
        }
        else {
            String message = getString(R.string.no_network_2);
            Util.showToast(this, message, Toast.LENGTH_SHORT);
        }
    }
}

