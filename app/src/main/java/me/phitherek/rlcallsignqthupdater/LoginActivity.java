package me.phitherek.rlcallsignqthupdater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Hashtable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
            final Context ctx = getApplicationContext();
            SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            String defaultValue = "";
            String currentToken = sharedPrefs.getString(getString(R.string.shared_preferences_current_token_key), defaultValue);
            if (!currentToken.equals("")) {
                CheckToken checkTokenTask = new CheckToken(ctx);
                checkTokenTask.execute(currentToken);
                while(!checkTokenTask.getFinished()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(checkTokenTask.getError() || !checkTokenTask.getResult()) {
                    finish();
                }
            }

            Button loginBtn = (Button) findViewById(R.id.loginButton);
            loginBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText callsignField = (EditText) findViewById(R.id.callsignField);
                    EditText passwordField = (EditText) findViewById(R.id.passwordField);
                    String callsign = callsignField.getText().toString();
                    String password = passwordField.getText().toString();
                    DoLogin doLoginTask = new DoLogin(ctx, callsign, password);
                    doLoginTask.execute();
                }
            });
    }

    @Override
    public void onBackPressed() {
        Intent mainActivity = new Intent(Intent.ACTION_MAIN);
        mainActivity.addCategory(Intent.CATEGORY_HOME);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainActivity);
    }

    public class CheckToken extends AsyncTask<String, Void, Void> {

        protected Boolean result;
        protected Boolean finished;
        protected Boolean error;
        protected Context ctx;

        public CheckToken(Context ctx) {
            finished = false;
            result = false;
            error = false;
            this.ctx = ctx;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Gson gson = new Gson();
                String checkUrls = "https://rlauth.phitherek.me/altapi/user_data";
                String query = "token=" + URLEncoder.encode(params[0], "UTF-8");
                URL checkUrl = new URL(checkUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlauth));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlauth", ca);
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                HttpsURLConnection con = (HttpsURLConnection)checkUrl.openConnection();
                con.setSSLSocketFactory(sslSocketFactory);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Length", String.valueOf(query.length()));
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("UserAgent", "RLCallsign QTH Updater Android");
                con.setDoOutput(true);
                con.setDoInput(true);
                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String response = input.readLine();
                input.close();
                if(con.getResponseCode() != 200) {
                    finished = true;
                    error = true;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if(parsedResponse.get("error") != null) {
                        String error = parsedResponse.get("error");
                        SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor spEditor = sharedPrefs.edit();
                        spEditor.putString(ctx.getString(R.string.shared_preferences_current_token_key), "");
                        spEditor.commit();
                        result = false;
                    } else {
                        result = true;
                    }
                    finished = true;
                }
            } catch(IOException | CertificateException | KeyStoreException | KeyManagementException | NoSuchAlgorithmException e) {
                e.printStackTrace();
                finished = true;
                error = true;
            }
            return null;
        }

        public Boolean getResult() {
            return result;
        }

        public Boolean getFinished() {
            return finished;
        }

        public Boolean getError() {
            return error;
        }
    }

    public class DoLogin extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String callsign;
        protected String password;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;

        public DoLogin(Context ctx, String callsign, String password) {
            this.ctx = ctx;
            error = false;
            this.callsign = callsign;
            this.password = password;
            exception = false;
            apiError = "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LinearLayout loginMainVL = (LinearLayout) findViewById(R.id.loginMainVL);
            TextView loginPleaseWaitTV = (TextView) findViewById(R.id.loginPleaseWaitTV);
            loginMainVL.setVisibility(View.GONE);
            loginPleaseWaitTV.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Gson gson = new Gson();
                String loginUrls = "https://rlauth.phitherek.me/altapi/login";
                String query = "callsign=" + URLEncoder.encode(callsign, "UTF-8") + "&password=" + URLEncoder.encode(password, "UTF-8");
                URL loginUrl = new URL(loginUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlauth));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlauth", ca);
                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
                SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                HttpsURLConnection con = (HttpsURLConnection) loginUrl.openConnection();
                con.setSSLSocketFactory(sslSocketFactory);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Length", String.valueOf(query.length()));
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("UserAgent", "RLCallsign QTH Updater Android");
                con.setDoOutput(true);
                con.setDoInput(true);
                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                BufferedReader input = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String response = input.readLine();
                input.close();
                if (con.getResponseCode() != 200) {
                    error = true;
                    return false;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if (parsedResponse.get("error") != null) {
                        apiError = parsedResponse.get("error");
                        return false;
                    } else {
                        SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor spEditor = sharedPrefs.edit();
                        spEditor.putString(getString(R.string.shared_preferences_current_token_key), parsedResponse.get("token"));
                        spEditor.commit();
                        return true;
                    }
                }
            } catch(IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                exception = true;
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                Intent intent = new Intent(getApplicationContext(), UpdateActivity.class);
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_logged_in), Toast.LENGTH_SHORT);
                toast.show();
                startActivity(intent);
                finish();
            } else {
                if(apiError != "") {
                    Toast toast = null;
                    try {
                        toast = Toast.makeText(ctx, ctx.getString(R.string.toast_error_response).concat(ctx.getString(R.string.class.getField("api_error_code_".concat(apiError)).getInt(null))), Toast.LENGTH_SHORT);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                    toast.show();
                } else if(exception) {
                    Toast toast = Toast.makeText(getApplicationContext(), getString(R.string.toast_exception), Toast.LENGTH_SHORT);
                    toast.show();
                } else if(error) {
                    Toast toast = Toast.makeText(ctx, ctx.getText(R.string.toast_error_connection), Toast.LENGTH_SHORT);
                    toast.show();
                }
                LinearLayout loginMainVL = (LinearLayout) findViewById(R.id.loginMainVL);
                TextView loginPleaseWaitTV = (TextView) findViewById(R.id.loginPleaseWaitTV);
                loginPleaseWaitTV.setVisibility(View.GONE);
                loginMainVL.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(result);
        }
    }
}
