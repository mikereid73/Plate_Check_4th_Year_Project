package c00112726.itcarlow.ie.finalyearproject.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import c00112726.itcarlow.ie.finalyearproject.tasks.callbacks.TaskCallbackJSON;

/**
 * Author: Michael Reid.
 * ID: C00112726
 * Email: c00112726@itcarlow.ie
 * Date: 05/04/2016
 */
public class ChangePasswordTask extends AsyncTask<String, Void, JSONObject> {

    private static final String TAG = "ChangePasswordTask";
    private static final String LOGIN_URL = "http://mikereid73.pythonanywhere.com/change_password";

    private TaskCallbackJSON mTaskCallback;
    private ProgressDialog mProgressDialog;

    public ChangePasswordTask(TaskCallbackJSON taskCallback) {
        mTaskCallback = taskCallback;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mProgressDialog = new ProgressDialog((Context) mTaskCallback);
        mProgressDialog.setMessage("Applying changes...");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
    }

    @Override
    protected JSONObject doInBackground(String... params) {
        String username = params[0];
        String oldPassword = params[1];
        String newPassword = params[2];

        JSONObject json = new JSONObject();
        try {
            json.put("username", username);
            json.put("old_password", oldPassword);
            json.put("new_password", newPassword);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        try {
            URL url = new URL(LOGIN_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000); /* milliseconds */
            urlConnection.setConnectTimeout(15000); /* milliseconds */
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            urlConnection.connect();

            // Post the json to the server
            DataOutputStream wr = new DataOutputStream(urlConnection.getOutputStream());
            wr.writeBytes(json.toString());
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

            return new JSONObject(sb.toString());

        }catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        catch (JSONException e) {
            Log.i(TAG, e.getMessage());
        }

        return null;
    }

    @Override
    protected void onPostExecute(JSONObject json) {
        super.onPostExecute(json);
        if(mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }

        mTaskCallback.onTaskComplete(json);
    }
}
