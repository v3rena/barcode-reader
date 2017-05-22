package com.google.android.gms.samples.vision.barcodereader;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class BarcodeResultActivity extends AppCompatActivity {

    ProgressDialog pd;
    private TextView PalmOilResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);
        Barcode barcode = getIntent().getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
        PalmOilResult = (TextView)findViewById(R.id.palm_oil_result);

        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode.rawValue + ".json";

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
                return connection.getInputStream();

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
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
            try (JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"))) {
                int hasPalmoil = readItem(reader);
                reader.close();
                PalmOilResult.setText(String.valueOf(hasPalmoil));
            }
        }

        private int readItem(JsonReader reader) throws  IOException {
            int hasPalmoil = 0;

            reader.beginObject();
            while (reader.hasNext()) {
                String FirstName = reader.nextName();
                if (FirstName.equals("product")) {
                reader.beginObject();
                    while (reader.hasNext()) {
                        String name = reader.nextName();
                        if (name.contains("palm_oil")) {
                            hasPalmoil = reader.nextInt();
                            if (hasPalmoil == 1) {
                                reader.endObject();
                                return hasPalmoil;
                            }
                        }
                        else if (name.contains("ingredients_text")) {
                            String ingredients = reader.nextString();
                            if ((ingredients.contains("palm oil")) || (ingredients.contains("palm-oil")) || (ingredients.contains("palm_oil")) || (ingredients.contains("Palmöl"))) {
                                hasPalmoil=1;
                                //reader.endObject();
                                return hasPalmoil;
                            }
                        }
                        else {
                            reader.skipValue();
                        }
                    }
                    //Notwendig
                    reader.endObject();
                }
                else {
                    reader.skipValue();
                }
            }
            reader.endObject();
            return hasPalmoil;
        }
    }
}






