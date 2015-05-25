package com.greenaddress.greenbits.ui.bitrefill;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.dd.CircularProgressButton;
import com.greenaddress.greenbits.ConnectivityObservable;
import com.greenaddress.greenbits.ui.ActionBarActivity;
import com.greenaddress.greenbits.ui.R;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

public class BitRefillActivity extends ActionBarActivity {

  private static final String BASE_URL = "https://api.bitrefill.com/v1";
  private static final String KEY = "73YY73YNJ284Y3MOVPHITD5A3";
  private static final String SECRET =
      "DVbfs3JNkoXlDnzeOIH7lge00eq5jg2MrxJwMX9MCPQ";
  private static final String BASIC_AUTH = String.format("Basic %s",
      Base64.encodeToString(String.format("%s:%s", KEY, SECRET).getBytes(),
          Base64.NO_WRAP));

  private static final String TAG = "BitRefillActivity";


  Handler handler;
  private EditText mobileNumberEdit;
  private boolean updatingPending, inProgress;

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putBoolean("inProgress", inProgress);
  }

  private String lookup_number(final String number) throws IOException {

    final URL url = new URL(String.format("%s/lookup_number?number=%s",
        BASE_URL, URLEncoder.encode(number, "utf-8")));

    final HttpURLConnection urlConnection = (HttpURLConnection) url
        .openConnection();

    try {

      urlConnection.setRequestProperty("Authorization", BASIC_AUTH);

      final InputStream in = new BufferedInputStream(urlConnection
          .getInputStream());

      if (!url.getHost().equals(urlConnection.getURL().getHost())) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
            (urlConnection.getURL().getHost())));
        return null;
      }

      final StringBuilder sb = new StringBuilder();
      final BufferedReader rd = new BufferedReader(new
          InputStreamReader(in));
      String line;

      while ((line = rd.readLine()) != null) {
        sb.append(line);
      }

      return sb.toString();
    } finally {
      urlConnection.disconnect();
    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getGAService() == null) {
      finish();
      return;
    }
    if (getGAApp().getConnectionObservable().getState() !=
        ConnectivityObservable.State.LOGGEDIN) {
      finish();
      return;
    }
    handler = new Handler();
    setContentView(R.layout.activity_bit_refill);

    mobileNumberEdit = (EditText) findViewById(R.id.mobileNumberEditText);

    // FIXEME: Prefill with user mobile number if set

    final CircularProgressButton checkButton = (CircularProgressButton)
        findViewById(R.id.checkNumberButton);
    final Activity act = this;
    final View.OnClickListener ocl = new View.OnClickListener() {

      @Override
      public void onClick(final View v) {

        new AsyncTask<Void, Void, String>() {
          @Override
          protected String doInBackground(final Void... Voids) {
            try {
              final String json = lookup_number(mobileNumberEdit
                  .getText().toString());
              Log.i(TAG, json);

              return json;

            } catch (final  IOException e) {
              e.printStackTrace();

            }
            return null;

          }

          protected void onPostExecute(final String result) {
            checkButton.setIndeterminateProgressMode(false);
            checkButton.setProgress(0);
            if (result == null) {
              Toast.makeText(act, "Number or country not supported",
                  Toast
                  .LENGTH_LONG).show();
            } else {
              final Intent i = new Intent(BitRefillActivity.this,
                  MobileOperatorPackagesActivity.class);
              i.putExtra("operator", result);
              startActivity(i);

            }
          }

        }.execute();
        checkButton.setIndeterminateProgressMode(true);
        checkButton.setProgress(50);
      }
    };

    checkButton.setOnClickListener(ocl);
    // restore button re-enabling callbacks
    if (savedInstanceState != null)

    {
      inProgress = savedInstanceState.getBoolean("inProgress");
      if (inProgress) {

      }

    }

  }

  private void updatePendingOrders() {
    if (getGAApp().getConnectionObservable().getState() !=
        ConnectivityObservable.State.LOGGEDIN) {
      return;
    }
    updatingPending = true;

  }

  @Override
  protected void onStop() {
    super.onStop();
    handler.removeCallbacksAndMessages(null);
    updatingPending = false;
  }


  @Override
  public void onPause() {
    super.onPause();
    handler.removeCallbacksAndMessages(null);
    updatingPending = false;
  }

  @Override
  public void onStart() {
    super.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!updatingPending) {
      updatePendingOrders();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(final Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.bitrefill, menu);
    return true;
  }


  @Override
  public boolean onOptionsItemSelected(final MenuItem item) {
    final int id = item.getItemId();
    if (id == R.id.action_bitrefill) {
      startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse
          ("https://www.bitrefill.com/")));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

}
