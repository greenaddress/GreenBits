package com.greenaddress.greenbits.ui.bitrefill;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.ActionBarActivity;
import com.greenaddress.greenbits.ui.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MobileOperatorPackagesActivity extends ActionBarActivity {

  private static final String TAG = "BirefillPkgPick";

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_mobile_operator_packages);
    final Intent intent = getIntent();
    final String result = intent.getExtras().getString("operator");
    final String mobileNumber = intent.getExtras().getString("number");

    final List<BitRefillPackage> pkgs = new ArrayList<>();
    final ListView listView = (ListView) findViewById(R.id.listView);
    final TextView operatorText = (TextView) findViewById(R.id.operator);

    final ImageView opImage = (ImageView) findViewById(R.id.imageView);


    JSONObject operator = null;
    try {
      operator = new JSONObject(result).getJSONObject("operator");
      final String logoImage = operator.getString("logoImage");
      new AsyncTask<Void, Void, Bitmap>() {
        protected Bitmap doInBackground(final Void... Voids) {
          try {
            final InputStream in = new java.net.URL(logoImage)
                .openStream();
            return BitmapFactory.decodeStream(in);
          } catch (final Exception e) {
            e.printStackTrace();
          }
          return null;
        }

        protected void onPostExecute(final Bitmap result) {
          opImage.setImageBitmap(result);
        }
      }.execute();


      final String operatorSlug = operator.getString("slug");
      final String operatorName = operator.getString("name");
      operatorText.setText(operatorName);

      final JSONArray packages = operator.getJSONArray("packages");
      final String currency = operator.getString("currency");
      for (int i = 0; i < packages.length(); ++i) {
        final JSONObject pkg = packages.getJSONObject(i);
        final String price = String.format("(= %s EUR)", pkg.getString
            ("eurPrice"));

        pkgs.add(new BitRefillPackage(pkg.getString("value"), price, pkg
            .getString
            ("satoshiPrice"), currency));

      }

      if (listView.getAdapter() != null) {
        ((ListBitRefillPkgsAdapter) listView.getAdapter()).clear();
        for (final BitRefillPackage pkg : pkgs) {
          ((ListBitRefillPkgsAdapter) listView.getAdapter()).add(pkg);
        }
        ((ListBitRefillPkgsAdapter) listView.getAdapter())
            .notifyDataSetChanged();
      } else {
        listView.setAdapter(new ListBitRefillPkgsAdapter(
            MobileOperatorPackagesActivity.this,
            R.layout.list_element_bitrefill, pkgs));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(final AdapterView<?> adapterView, final View
              view, final int position, final long l) {

            final BitRefillPackage pkg = (BitRefillPackage) listView
                .getAdapter().getItem(position);
            Log.i(TAG, pkg.satoshiPrice);
            final Intent intent = new Intent(MobileOperatorPackagesActivity
                .this,
                BitRefillPaymentActivity.class);

            intent.putExtra("number", mobileNumber);
            intent.putExtra("valuePackage", pkg.value);
            intent.putExtra("operatorSlug", operatorSlug);
            intent.putExtra("operatorName", operatorName);
            intent.putExtra("currency", currency);
            intent.putExtra("satoshiPrice", pkg.satoshiPrice);

            startActivity(intent);
          }
        });
      }

    } catch (final JSONException e) {
      e.printStackTrace();
    }
  }
}
