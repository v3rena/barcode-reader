package com.google.android.gms.samples.vision.barcodereader;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;

import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class BarcodeResultActivity extends AppCompatActivity {

    private Barcode barcode;
    private String url;
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);
        barcode = getIntent().getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);

        url = "https://world.openfoodfacts.org/api/v0/product/" + barcode.rawValue + ".json";

        new JsonTask().execute(url);
    }

    private class JsonTask extends AsyncTask<String, String, InputStream> {

        protected void onPreExecute() {
            super.onPreExecute();

            pd = new ProgressDialog(BarcodeResultActivity.this);
            pd.setMessage("Please wait");
            pd.setCancelable(false);
            pd.show();
        }

        protected InputStream doInBackground(String... params) {

            HttpURLConnection connection = null;
            BufferedReader reader = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();


                InputStream stream = connection.getInputStream();

                /*reader = new BufferedReader(new InputStreamReader(stream));

                StringBuffer buffer = new StringBuffer();
                String line = "";

                while ((line = reader.readLine()) != null) {
                    buffer.append(line+"\n");
                    Log.d("Response: ", "> " + line);   //here u ll get whole response...... :-)

                }*/

                return stream;


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(InputStream result) {
            super.onPostExecute(result);
            if (pd.isShowing()){
                pd.dismiss();
            }
            try {
                parseJSON(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void parseJSON(InputStream inputStream) throws IOException {
            JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
            try {
                //TODO: https://developer.android.com/reference/android/util/JsonReader.html
                //return readMessagesArray(reader);
            } finally {
                reader.close();
            }
        }
    }
}






