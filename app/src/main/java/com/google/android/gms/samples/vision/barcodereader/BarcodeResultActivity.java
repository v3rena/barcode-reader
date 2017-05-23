package com.google.android.gms.samples.vision.barcodereader;

import android.app.ProgressDialog;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.JsonReader;
import android.util.JsonToken;
import android.widget.ImageView;
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
    private ImageView ResultImage;
    private Drawable CrossPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_result);
        Barcode barcode = getIntent().getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
        PalmOilResult = (TextView)findViewById(R.id.palm_oil_result);
        ResultImage = (ImageView)findViewById(R.id.result_image);


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

                switch(hasPalmoil){
                    case 0:
                        PalmOilResult.setText(R.string.result_text_palm_oil_free);
                        ResultImage.setImageResource(R.drawable.check);
                        break;
                    case 1:
                        PalmOilResult.setText(R.string.result_contains_palm_oil);
                        ResultImage.setImageResource(R.drawable.cross);
                        break;
                    case 404:
                        PalmOilResult.setText(R.string.result_product_not_found);
                        ResultImage.setImageResource(R.drawable.search);
                        break;
                    default:
                        PalmOilResult.setText(R.string.result_technical_problems);
                        ResultImage.setImageResource(R.drawable.wrench);
                }
            }
        }

        private int readItem(JsonReader reader) throws  IOException {
            int resultCode = 0;
            reader.beginObject();
            while (reader.hasNext()) {
                if (reader.peek().equals(JsonToken.NAME)) {
                    //
                    String FirstName = reader.nextName();

                    if(FirstName.equals("status")) {
                        if (reader.peek().equals(JsonToken.NUMBER)) {
                            int status = reader.nextInt();
                            if (status == 0) {
                                resultCode = 404;
                                break;
                            }
                        }
                        else {
                            reader.skipValue();
                        }
                    }
                    else if (FirstName.equals("product")) {
                        reader.beginObject();
                        while (reader.hasNext()) {
                            if (reader.peek().equals(JsonToken.NAME)) {
                                String name = reader.nextName();
                                if (name.contains("palm_oil")) {
                                    //
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

                                    //
                                }
                                else if (name.contains("ingredients_text")) {
                                    //
                                    if (reader.peek().equals(JsonToken.STRING)) {
                                        String ingredients = reader.nextString();
                                        if ((ingredients.contains("palm oil")) || (ingredients.contains("palm-oil")) || (ingredients.contains("palm_oil")) || (ingredients.contains("Palmöl"))) {
                                            resultCode = 1;
                                            //reader.endObject();
                                            //return resultCode;
                                            break;
                                        }

                                    }
                                    else {
                                        reader.skipValue();
                                    }
                                    //
                                } else {
                                    reader.skipValue();
                                }
                            } else {
                                reader.skipValue();
                            }
                        }
                        //Irgendwo endObject(), aber nicht hier?
                        //reader.endObject();
                    } else {
                        reader.skipValue();
                    }
                }
                //
            }
            //reader.endObject();
            return resultCode;
        }
    }
}


/*
     if(FirstName.equals("status")) {
                    int statusCode = reader.nextInt();
                    if(statusCode == 0) {
                        hasPalmoil = 404;
                        return hasPalmoil;
                    }
                }
 */



