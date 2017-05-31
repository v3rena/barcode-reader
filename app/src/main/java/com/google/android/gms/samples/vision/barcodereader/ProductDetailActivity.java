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
import java.util.HashMap;
import java.util.Map;

import static com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity.BarcodeObject;

public class ProductDetailActivity extends AppCompatActivity {

    private TextView ProductDetails;
    private TextView DetailsHeadline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        ProductDetails = (TextView)findViewById(R.id.show_details);
        DetailsHeadline = (TextView)findViewById(R.id.details_headline);
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
                Map<String, String> results = parseJSON(reader);
                reader.close();
                printResult(results);
            }
        }

        private Map<String, String> parseJSON(JsonReader reader) throws  IOException {
            Map<String, String> results = new HashMap<String, String>();
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.peek().equals(JsonToken.NAME)) {
                    String FirstName = reader.nextName();
                    switch (FirstName) {
                        case "product":
                            reader.beginObject();
                            while (reader.hasNext()) {
                                if (reader.peek().equals(JsonToken.NAME)) {
                                    String name = reader.nextName();
                                    if (name.equals("product_name")) {
                                        if (reader.peek().equals(JsonToken.STRING)) {
                                            String productName = reader.nextString();
                                            if (!results.containsKey("Name")) {
                                                results.put("Name", productName);
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    else if (name.equals("traces")) {
                                        if (reader.peek().equals(JsonToken.STRING)) {
                                            String traces = reader.nextString();
                                            if (!results.containsKey("Traces")) {
                                                results.put("Traces", traces);
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    }
                                    else if (name.contains("ingredients_text")) {
                                        switch (name) {
                                            case "ingredients_text_de":
                                                if (reader.peek().equals(JsonToken.STRING)) {
                                                    String ingredients = reader.nextString();
                                                    results.put("Ingredients", ingredients);
                                                } else {
                                                    reader.skipValue();
                                                }
                                                break;
                                            case "ingredients_text":
                                                if (reader.peek().equals(JsonToken.STRING)) {
                                                    String ingredients = reader.nextString();
                                                    if (!results.containsKey("Ingredients")) {
                                                        results.put("Ingredients", ingredients);
                                                    }
                                                } else {
                                                    reader.skipValue();
                                                }
                                                break;
                                        }
                                    }
                                    else if (name.contains("palm_oil")) {
                                        if (reader.peek().equals(JsonToken.NUMBER)) {
                                            int resultCode = reader.nextInt();
                                            if (!results.containsKey("Ingredients that may be from palm oil")) {
                                                if (resultCode == 1) {
                                                    results.put("Ingredients that may be from palm oil", "Yes");
                                                } else if (resultCode == 0) {
                                                    results.put("Ingredients that may be from palm oil", "No");
                                                }
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
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
            }
            return results;
        }

        private void printResult(Map<String, String> results) {
            DetailsHeadline.setText(results.get("Name"));
            String ingredients = results.get("Ingredients");
            String containsPalmoil = results.get("Ingredients that may be from palm oil");
            String traces = results.get("Traces");
            ProductDetails.setText(ingredients + "\n\n" + "Traces: "  + traces + "\n\n" + "Ingredients that may be from palm oil: " + containsPalmoil);
        }
    }
}

