package com.google.android.gms.samples.vision.barcodereader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.JsonToken;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity.BarcodeObject;

public class ProductDetailActivity extends AppCompatActivity {

    private TextView ProductDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        Barcode barcode = getIntent().getParcelableExtra(BarcodeObject);
        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode.rawValue + ".json";
        new JsonTask().execute(url);
    }

    private class JsonTask extends AsyncTask<String, String, InputStream> {

        protected InputStream doInBackground(String... params) {
            HttpURLConnection connection = null;
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
            try {
                readJSON(result);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void readJSON(InputStream inputStream) throws IOException {
            try (JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"))) {
                parseJSON(reader);
                reader.close();
            }
        }

        private void parseJSON(JsonReader reader) throws  IOException {
            int resultCode = 0;
            reader.beginObject();
            loop:
            while (reader.hasNext()) {
                if (reader.peek().equals(JsonToken.NAME)) {
                    String FirstName = reader.nextName();
                    switch (FirstName) {
                        case "status":
                            if (reader.peek().equals(JsonToken.NUMBER)) {
                                int status = reader.nextInt();
                                if (status == 0) {
                                    resultCode = 404;
                                    break loop;
                                }
                            } else {
                                reader.skipValue();
                            }
                            break;
                        case "product":
                            reader.beginObject();
                            while (reader.hasNext()) {
                                if (reader.peek().equals(JsonToken.NAME)) {
                                    String name = reader.nextName();
                                    if (name.contains("palm_oil")) {
                                        if (reader.peek().equals(JsonToken.NUMBER)) {
                                            resultCode = reader.nextInt();
                                            if (resultCode == 1) {
                                                //reader.endObject();
                                                //return resultCode;
                                                break;
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    } else if (name.contains("ingredients_text")) {
                                        if (reader.peek().equals(JsonToken.STRING)) {
                                            String ingredients = reader.nextString();
                                            if ((ingredients.contains("palm oil")) || (ingredients.contains("palm-oil")) || (ingredients.contains("palm_oil")) || (ingredients.contains("Palmöl"))) {
                                                resultCode = 1;
                                                //reader.endObject();
                                                //return resultCode;
                                                break;
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    } else {
                                        reader.skipValue();
                                    }
                                } else {
                                    reader.skipValue();
                                }
                            }
                            //reader.endObject();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
            }
            //reader.endObject();
        }
    }
}

