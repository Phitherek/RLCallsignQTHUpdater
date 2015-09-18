package me.phitherek.rlcallsignqthupdater;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
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
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class UpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update);
        LinearLayout mainVL = (LinearLayout) findViewById(R.id.mainVL);
        TextView pleaseWaitTV = (TextView) findViewById(R.id.pleaseWaitTV);
        mainVL.setVisibility(View.GONE);
        pleaseWaitTV.setVisibility(View.VISIBLE);
        Context ctx = getApplicationContext();
        SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        String defaultValue = "";
        final String currentToken = sharedPrefs.getString(getString(R.string.shared_preferences_current_token_key), defaultValue);
        if(currentToken.equals("")) {
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
        }
        GetCallsign getCallsignTask = new GetCallsign(ctx);
        getCallsignTask.execute(currentToken);
        while(!getCallsignTask.getFinished()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(getCallsignTask.getError()) {
            Intent mainActivity = new Intent(Intent.ACTION_MAIN);
            mainActivity.addCategory(Intent.CATEGORY_HOME);
            mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainActivity);
        } else {
            if(getCallsignTask.getResult() == "") {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
            } else {
                TextView callsignContent = (TextView) findViewById(R.id.callsignContent);
                callsignContent.setText(getCallsignTask.getResult());
                GetCallsignInfo getCallsignInfoTask = new GetCallsignInfo(ctx);
                getCallsignInfoTask.execute(currentToken);
                while(!getCallsignInfoTask.getFinished()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(getCallsignInfoTask.getError()) {
                    Intent mainActivity = new Intent(Intent.ACTION_MAIN);
                    mainActivity.addCategory(Intent.CATEGORY_HOME);
                    mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(mainActivity);
                } else {
                    EditText currentQTHField = (EditText) findViewById(R.id.currentQTHField);
                    EditText currentQTHLocatorField = (EditText) findViewById(R.id.currentQTHLocatorField);
                    currentQTHField.setText(getCallsignInfoTask.getCurrentQth());
                    currentQTHLocatorField.setText(getCallsignInfoTask.getCurrentQthLocator());
                    Button logoutButton = (Button) findViewById(R.id.logoutBtn);
                    logoutButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DoLogout doLogoutTask = new DoLogout(getApplicationContext(), currentToken);
                            doLogoutTask.execute();
                        }
                    });
                    Button updateButton = (Button) findViewById(R.id.updateBtn);
                    updateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText currentQTHField = (EditText) findViewById(R.id.currentQTHField);
                            EditText currentQTHLocatorField = (EditText) findViewById(R.id.currentQTHLocatorField);
                            String currentQTH = currentQTHField.getText().toString();
                            String currentQTHLocator = currentQTHLocatorField.getText().toString();
                            DoUpdate doUpdateTask = new DoUpdate(getApplicationContext(), currentToken, currentQTH, currentQTHLocator);
                            doUpdateTask.execute();
                        }
                    });
                    Button fromAddressButton = (Button) findViewById(R.id.fromAddressBtn);
                    fromAddressButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText currentQTHField = (EditText) findViewById(R.id.currentQTHField);
                            String currentQTH = currentQTHField.getText().toString();
                            DoRLQTHAddressQuery doRLQTHAddressQueryTask = new DoRLQTHAddressQuery(getApplicationContext(), currentQTH);
                            doRLQTHAddressQueryTask.execute();
                        }
                    });
                    Button fromLocatorButton = (Button) findViewById(R.id.fromLocatorBtn);
                    fromLocatorButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            EditText currentQTHLocatorField = (EditText) findViewById(R.id.currentQTHLocatorField);
                            String currentQTHLocator = currentQTHLocatorField.getText().toString();
                            DoRLQTHReverseQuery doRLQTHReverseQueryTask = new DoRLQTHReverseQuery(getApplicationContext(), currentQTHLocator);
                            doRLQTHReverseQueryTask.execute();
                        }
                    });
                    Button geolocateButton = (Button) findViewById(R.id.geolocateBtn);
                    geolocateButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DoGeolocate doGeolocateTask = new DoGeolocate(getApplicationContext());
                            doGeolocateTask.execute();
                        }
                    });
                    pleaseWaitTV.setVisibility(View.GONE);
                    mainVL.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Intent mainActivity = new Intent(Intent.ACTION_MAIN);
        mainActivity.addCategory(Intent.CATEGORY_HOME);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(mainActivity);
    }

    public class GetCallsign extends AsyncTask<String, Void, Void> {

        protected String result;
        protected Boolean finished;
        protected Boolean error;
        protected Context ctx;

        public GetCallsign(Context ctx) {
            finished = false;
            result = "";
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
                        SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor spEditor = sharedPrefs.edit();
                        spEditor.putString(ctx.getString(R.string.shared_preferences_current_token_key), "");
                        spEditor.commit();
                    } else {
                        Hashtable<String, String> userData = gson.fromJson(gson.toJson(parsedResponse.get("user")), new Hashtable<String, String>().getClass());
                        result = userData.get("callsign");
                        error = false;
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

        public String getResult() {
            return result;
        }

        public Boolean getFinished() {
            return finished;
        }

        public Boolean getError() {
            return error;
        }
    }

    public class GetCallsignInfo extends AsyncTask<String, Void, Void> {

        protected String currentQth;
        protected String currentQthLocator;
        protected Boolean finished;
        protected Boolean error;
        protected Context ctx;

        public GetCallsignInfo(Context ctx) {
            finished = false;
            currentQth = "";
            currentQthLocator = "";
            error = false;
            this.ctx = ctx;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Gson gson = new Gson();
                String checkUrls = "https://rlcallsign.phitherek.me/api/user_callsign_info";
                String query = "token=" + URLEncoder.encode(params[0], "UTF-8");
                URL checkUrl = new URL(checkUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlcallsign));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlcallsign", ca);
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

                    } else {
                        Hashtable<String, String> callsignInfo = gson.fromJson(gson.toJson(parsedResponse.get("info")), new Hashtable<String, String>().getClass());
                        currentQth = callsignInfo.get("current_qth");
                        currentQthLocator = callsignInfo.get("current_qth_locator");
                        error = false;
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



        public Boolean getFinished() {
            return finished;
        }

        public Boolean getError() {
            return error;
        }

        public String getCurrentQth() {
            return currentQth;
        }

        public String getCurrentQthLocator() {
            return currentQthLocator;
        }
    }

    public class DoLogout extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String token;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;

        public DoLogout(Context ctx, String token) {
            this.ctx = ctx;
            error = false;
            this.token = token;
            apiError = "";
            exception = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LinearLayout mainVL = (LinearLayout) findViewById(R.id.mainVL);
            TextView pleaseWaitTV = (TextView) findViewById(R.id.pleaseWaitTV);
            mainVL.setVisibility(View.GONE);
            pleaseWaitTV.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Gson gson = new Gson();
                String loginUrls = "https://rlauth.phitherek.me/altapi/logout";
                String query = "token=" + URLEncoder.encode(token, "UTF-8");
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
                    return false;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if (parsedResponse.get("error") != null) {
                        apiError = parsedResponse.get("error");
                        return false;
                    } else if(parsedResponse.get("success").equals("success")) {
                        SharedPreferences sharedPrefs = ctx.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                        SharedPreferences.Editor spEditor = sharedPrefs.edit();
                        spEditor.putString(getString(R.string.shared_preferences_current_token_key), "");
                        spEditor.commit();
                        return true;
                    }
                }
            } catch(IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                exception = true;
                e.printStackTrace();
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_logged_out), Toast.LENGTH_SHORT);
                toast.show();
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
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
                LinearLayout mainVL = (LinearLayout) findViewById(R.id.mainVL);
                TextView pleaseWaitTV = (TextView) findViewById(R.id.pleaseWaitTV);
                pleaseWaitTV.setVisibility(View.GONE);
                mainVL.setVisibility(View.VISIBLE);
            }
            super.onPostExecute(result);
        }
    }

    public class DoUpdate extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String token;
        protected String currentQth;
        protected String currentQthLocator;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;

        public DoUpdate(Context ctx, String token, String currentQth, String currentQthLocator) {
            this.ctx = ctx;
            error = false;
            this.token = token;
            apiError = "";
            exception = false;
            this.currentQth = currentQth;
            this.currentQthLocator = currentQthLocator;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            LinearLayout mainVL = (LinearLayout) findViewById(R.id.mainVL);
            TextView pleaseWaitTV = (TextView) findViewById(R.id.pleaseWaitTV);
            mainVL.setVisibility(View.GONE);
            pleaseWaitTV.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Gson gson = new Gson();
                String loginUrls = "https://rlcallsign.phitherek.me/api/update_current_qth";
                String query = "token=" + URLEncoder.encode(token, "UTF-8");
                if(!currentQth.equals("")) {
                    query = query.concat("&current_qth=" + URLEncoder.encode(currentQth, "UTF-8"));
                }
                if(!currentQthLocator.equals("")) {
                    query = query.concat("&current_qth_locator=" + URLEncoder.encode(currentQthLocator, "UTF-8"));
                }
                URL loginUrl = new URL(loginUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlcallsign));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlcallsign", ca);
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
                    return false;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if (parsedResponse.get("error") != null) {
                        apiError = parsedResponse.get("error");
                        return false;
                    } else if(parsedResponse.get("success").equals("success")) {
                        return true;
                    }
                }
            } catch(IOException | CertificateException | KeyStoreException | NoSuchAlgorithmException | KeyManagementException e) {
                exception = true;
                e.printStackTrace();
                return false;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if(result) {
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_updated), Toast.LENGTH_SHORT);
                toast.show();
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
            }
            LinearLayout mainVL = (LinearLayout) findViewById(R.id.mainVL);
            TextView pleaseWaitTV = (TextView) findViewById(R.id.pleaseWaitTV);
            pleaseWaitTV.setVisibility(View.GONE);
            mainVL.setVisibility(View.VISIBLE);
            super.onPostExecute(result);
        }
    }

    public class DoRLQTHAddressQuery extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String currentQth;
        protected String currentQthLocator;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;

        public DoRLQTHAddressQuery(Context ctx, String currentQth) {
            this.ctx = ctx;
            error = false;
            apiError = "";
            exception = false;
            this.currentQth = currentQth;
            currentQthLocator = "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Gson gson = new Gson();
                String loginUrls = "https://rlqth.phitherek.me/api/address";
                String query = "address=" + URLEncoder.encode(currentQth, "UTF-8");
                URL loginUrl = new URL(loginUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlqth));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlqth", ca);
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
                    return false;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if (parsedResponse.get("error") != null) {
                        apiError = parsedResponse.get("error");
                        return false;
                    } else {
                        Hashtable<String, String> parsedLocation = gson.fromJson(gson.toJson(parsedResponse.get("address_location")), new Hashtable<String, String>().getClass());
                        currentQthLocator = parsedLocation.get("locator");
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
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_query_successful), Toast.LENGTH_SHORT);
                toast.show();
                EditText currentQTHLocatorField = (EditText) findViewById(R.id.currentQTHLocatorField);
                currentQTHLocatorField.setText(currentQthLocator);
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
            }
            super.onPostExecute(result);
        }
    }

    public class DoRLQTHReverseQuery extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String currentQth;
        protected String currentQthLocator;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;

        public DoRLQTHReverseQuery(Context ctx, String currentQthLocator) {
            this.ctx = ctx;
            error = false;
            apiError = "";
            exception = false;
            this.currentQthLocator = currentQthLocator;
            currentQth = "";
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Gson gson = new Gson();
                String loginUrls = "https://rlqth.phitherek.me/api/reverse";
                String query = "locator=" + URLEncoder.encode(currentQthLocator, "UTF-8");
                URL loginUrl = new URL(loginUrls);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlqth));
                Certificate ca;
                try {
                    ca = cf.generateCertificate(caInput);
                } finally {
                    caInput.close();
                }
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("rlqth", ca);
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
                    return false;
                } else {
                    Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                    if (parsedResponse.get("error") != null) {
                        apiError = parsedResponse.get("error");
                        return false;
                    } else {
                        Hashtable<String, String> parsedLocation = gson.fromJson(gson.toJson(parsedResponse.get("reverse_location")), new Hashtable<String, String>().getClass());
                        currentQth = parsedLocation.get("reverse_address");
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
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_query_successful), Toast.LENGTH_SHORT);
                toast.show();
                EditText currentQTHField = (EditText) findViewById(R.id.currentQTHField);
                currentQTHField.setText(currentQth);
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
            }
            super.onPostExecute(result);
        }
    }

    public class DoGeolocate extends AsyncTask<Void, Void, Boolean> {

        protected Context ctx;
        protected String currentQth;
        protected String currentQthLocator;
        protected Boolean error;
        protected Boolean exception;
        protected String apiError;
        protected GoogleApiClient googleApiClient;
        protected Location lastLocation;
        protected Geocoder geocoder;

        public DoGeolocate(Context ctx) {
            this.ctx = ctx;
            error = false;
            apiError = "";
            exception = false;
            currentQthLocator = "";
            currentQth = "";
            googleApiClient = new GoogleApiClient.Builder(this.ctx).addApi(LocationServices.API).build();
            geocoder = new Geocoder(this.ctx, Locale.getDefault());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                googleApiClient.blockingConnect();
                if(googleApiClient.isConnected()) {
                    lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    if (lastLocation != null) {
                        Gson gson = new Gson();
                        String loginUrls = "https://rlqth.phitherek.me/api/pure";
                        String query = "latitude=" + URLEncoder.encode(String.valueOf(lastLocation.getLatitude()), "UTF-8") + "&longitude=" + URLEncoder.encode(String.valueOf(lastLocation.getLongitude()), "UTF-8");
                        URL loginUrl = new URL(loginUrls);
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        InputStream caInput = new BufferedInputStream(ctx.getResources().openRawResource(R.raw.rlqth));
                        Certificate ca;
                        try {
                            ca = cf.generateCertificate(caInput);
                        } finally {
                            caInput.close();
                        }
                        String keyStoreType = KeyStore.getDefaultType();
                        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                        keyStore.load(null, null);
                        keyStore.setCertificateEntry("rlqth", ca);
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
                            return false;
                        } else {
                            Hashtable<String, String> parsedResponse = gson.fromJson(response, new Hashtable<String, String>().getClass());
                            if (parsedResponse.get("error") != null) {
                                apiError = parsedResponse.get("error");
                                return false;
                            } else {
                                Hashtable<String, String> parsedLocation = gson.fromJson(gson.toJson(parsedResponse.get("pure_location")), new Hashtable<String, String>().getClass());
                                currentQthLocator = parsedLocation.get("locator");
                                List<Address> addresses = null;
                                try {
                                    addresses = geocoder.getFromLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), 1);
                                } catch(IOException e) {
                                    exception = true;
                                    e.printStackTrace();
                                    return false;
                                } catch(IllegalArgumentException e) {
                                    exception = true;
                                    e.printStackTrace();
                                    return false;
                                }
                                if(addresses == null || addresses.size() == 0) {
                                    currentQth = "";
                                    return true;
                                } else {
                                    Address address = addresses.get(0);
                                    ArrayList<String> addressFragments = new ArrayList<String>();
                                    for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                                        addressFragments.add(address.getAddressLine(i));
                                    }
                                    currentQth = TextUtils.join(System.getProperty("line.separator"), addressFragments);
                                    return true;
                                }
                            }
                        }
                    } else {
                        apiError = "emptycoordinates";
                        return false;
                    }
                } else {
                    error = true;
                    return false;
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
                Toast toast = Toast.makeText(ctx, getString(R.string.toast_geolocation_successful), Toast.LENGTH_SHORT);
                toast.show();
                EditText currentQTHField = (EditText) findViewById(R.id.currentQTHField);
                EditText currentQTHLocatorField = (EditText) findViewById(R.id.currentQTHLocatorField);
                currentQTHField.setText(currentQth);
                currentQTHLocatorField.setText(currentQthLocator);
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
            }
            super.onPostExecute(result);
        }
    }
}
