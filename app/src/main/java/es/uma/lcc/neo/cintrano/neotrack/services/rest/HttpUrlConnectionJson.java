package es.uma.lcc.neo.cintrano.neotrack.services.rest;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

public class HttpUrlConnectionJson {
    private static final String TAG = "HttpUrlConnectionJson";

    /**
     * Send post method from a JSON as string
     * @param urlPath URL for the API call
     * @param stringJson String of a JSON
     * @return response
     */
    public static String sendHTTPData(String urlPath, String method, String stringJson) {
        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
            TrustManager[] trustAllCerts = new TrustManager[]{new InsecureTrustManager()};
            sc.init(null, trustAllCerts, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

        if (sc != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                // Dummy verification
                return true;
            }
        };
        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);


        Log.i(TAG, "sendHTTPData");
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlPath);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            if (method.equals("POST")) {
                connection.setDoOutput(true);
                connection.setDoInput(true);

                OutputStreamWriter streamWriter = new OutputStreamWriter(connection.getOutputStream());
                streamWriter.write(stringJson);
                streamWriter.flush();
            }
            StringBuilder stringBuilder = new StringBuilder();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK){
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(streamReader);
                String response;
                while ((response = bufferedReader.readLine()) != null) {
                    stringBuilder.append(response);
                }
                bufferedReader.close();

                Log.i(TAG, stringBuilder.toString());
                return stringBuilder.toString();
            } else {
                Log.e(TAG, connection.getResponseMessage());
                return null;
            }
        } catch (ProtocolException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return null;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
            return null;
        } finally {
            if (connection != null){
                connection.disconnect();
            }
        }
    }
}
