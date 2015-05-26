package com.greenaddress.greenbits.ui.bitrefill;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.greenaddress.greenbits.ui.R;

import java.util.List;

public class ListBitRefillPkgsAdapter extends ArrayAdapter<BitRefillPackage> {

  public ListBitRefillPkgsAdapter(final Context context, final int resource,
      final List<BitRefillPackage> objects) {
    super(context, resource, objects);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final BitRefillPackage current = getItem(position);
    Holder holder;
    View returnedView;

    if (convertView == null) {
      final LayoutInflater inflater = LayoutInflater.from(getContext());
      returnedView = inflater.inflate(R.layout.list_element_bitrefill, parent,
          false);
      holder = new Holder();
      holder.current = current;
      holder.value = (TextView) returnedView.findViewById(R.id.pkgValue);
      holder.btcValue = (TextView) returnedView.findViewById(R.id.btcValue);
      holder.eurValue = (TextView) returnedView.findViewById(R.id.eurValue);

      returnedView.setTag(holder);
    } else {
      returnedView = convertView;
      holder = (Holder) returnedView.getTag();
    }
    final String displayValue = String.format("%s %s", current.value,
        current.currency);
    holder.value.setText(displayValue);
    holder.eurValue.setText(current.eurPrice);
    holder.btcValue.setText(current.satoshiPrice);
    return returnedView;
  }

  private static class Holder {
    protected BitRefillPackage current;
    protected TextView value;
    protected TextView btcValue;
    protected TextView eurValue;
  }

}
