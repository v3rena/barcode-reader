package com.google.android.gms.samples.vision.barcodereader;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.JsonToken;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.android.gms.samples.vision.barcodereader.BarcodeCaptureActivity.BarcodeObject;
public class BarcodeResultActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView PalmOilResult;
    private ImageView ResultImage;
    private Barcode barcode;
    private Button ShowDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);
        barcode = getIntent().getParcelableExtra(BarcodeObject);
        PalmOilResult = (TextView)findViewById(R.id.palm_oil_result);
        ResultImage = (ImageView)findViewById(R.id.result_image);
        ShowDetails = (Button)findViewById(R.id.show_details);
        findViewById(R.id.show_details).setOnClickListener(this);

        String url = "https://world.openfoodfacts.org/api/v0/product/" + barcode.rawValue + ".json";
        new JsonTask().execute(url);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.show_details) {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(BarcodeObject, barcode);
            startActivity(intent);
        }
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
                int resultCode = parseJSON(reader);
                reader.close();
                setResult(resultCode);
            }
        }

        private int parseJSON(JsonReader reader) throws  IOException {
            int resultCode = 999;
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
                                            int productsFromPalmOil = reader.nextInt();
                                            if (productsFromPalmOil == 1) {
                                                resultCode = 1;
                                                break;
                                            }
                                            else if (productsFromPalmOil == 0) {
                                                resultCode = 0;
                                            }
                                        } else {
                                            reader.skipValue();
                                        }
                                    } else if (name.contains("ingredients_text")) {
                                        if (reader.peek().equals(JsonToken.STRING)) {
                                            String ingredients = reader.nextString();
                                            if ((ingredients.contains("palm oil")) || (ingredients.contains("palm-oil")) || (ingredients.contains("palm_oil")) || (ingredients.contains("Palmöl"))) {
                                                resultCode = 1;
                                                break;
                                            }
                                            else {
                                                resultCode = 0;
                                            }
                                        } else if (reader.peek().equals((JsonToken.NULL))) {
                                            resultCode = 206;
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
            return resultCode;
        }

        private void setResult(int resultCode) {
            switch(resultCode){
                case 0:
                    setResultPalmOilFree();
                    break;
                case 1:
                    setResultContainsPalmOil();
                    break;
                case 404:
                    setResultProductNotFound();
                    break;
                case 206:
                    setResultInfoNotAvailable();
                    break;
                default:
                    setResultError();
            }
        }

        private void setResultContainsPalmOil() {
            PalmOilResult.setText(R.string.result_contains_palm_oil);
            ResultImage.setImageResource(R.drawable.cross);
            ShowDetails.setVisibility(Button.VISIBLE);
        }

        private void setResultPalmOilFree() {

            PalmOilResult.setText(R.string.result_text_palm_oil_free);
            ResultImage.setImageResource(R.drawable.check);
            ShowDetails.setVisibility(Button.VISIBLE);
        }

        private void setResultProductNotFound() {
            PalmOilResult.setText(R.string.result_product_not_found);
            ResultImage.setImageResource(R.drawable.search);
        }

        private void setResultInfoNotAvailable() {
            PalmOilResult.setText(R.string.result_text_info_not_available);
            ResultImage.setImageResource(R.drawable.search);
            ShowDetails.setVisibility(Button.VISIBLE);
        }

        private void setResultError() {
            PalmOilResult.setText(R.string.result_technical_problems);
            ResultImage.setImageResource(R.drawable.wrench);
        }
    }
}


