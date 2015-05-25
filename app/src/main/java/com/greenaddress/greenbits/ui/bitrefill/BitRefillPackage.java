package com.greenaddress.greenbits.ui.bitrefill;

import java.io.Serializable;

public class BitRefillPackage implements Serializable {

  public final String value;
  public final String eurPrice;
  public final String satoshiPrice;

  public BitRefillPackage(final String value, final String eurPrice, final
      String satoshiPrice) {
    this.value = value;
    this.eurPrice = eurPrice;
    this.satoshiPrice = satoshiPrice;
  }
}
