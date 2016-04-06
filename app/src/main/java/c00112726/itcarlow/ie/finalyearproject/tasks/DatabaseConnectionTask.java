package c00112726.itcarlow.ie.finalyearproject.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import c00112726.itcarlow.ie.finalyearproject.misc.NumberPlate;

/**
 * Author: Michael Reid
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 26/03/2016
 */
public class DatabaseConnectionTask extends AsyncTask<NumberPlate, String, String> {

    private static final String TAG = "DatabaseConnectionTask";
    private static final String LOGIN_URL = "http://mikereid73.pythonanywhere.com/post";
    private TaskCallback mTaskCallback;
    private ProgressDialog mProgressDialog;

    public DatabaseConnectionTask(TaskCallback taskCallback) {
        mTaskCallback = taskCallback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mProgressDialog = new ProgressDialog((Context) mTaskCallback);
        mProgressDialog.setMessage("Sending to database...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected String doInBackground(NumberPlate... params) {

        try {
            NumberPlate reg = params[0];
            SharedPreferences sharedPreferences = ((Context)mTaskCallback).getSharedPreferences("login", Context.MODE_PRIVATE);
            String username = sharedPreferences.getString("username", "admin");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("confirmed_reg_number", reg.toString());
            jsonObject.put("app_guess_reg_number", reg.getWrongGuess());
            jsonObject.put("username", username);

            URL url = new URL(LOGIN_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /*milliseconds*/);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            //make some HTTP header nicety
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            //urlConnection.setRequestProperty("X-Requested-With", "XMLHttpRequest");

            //open
            urlConnection.connect();

            // Post the json to the server
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(jsonObject.toString());
            wr.flush();
            wr.close();

            // Receive the response from the server
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            urlConnection.disconnect();
            reader.close();
            in.close();

            return sb.toString();
        }
        catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if(mTaskCallback == null) { return; }
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        mTaskCallback.onTaskComplete();

    }
}
