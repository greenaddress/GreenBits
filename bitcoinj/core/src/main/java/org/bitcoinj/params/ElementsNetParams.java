/*
 * Copyright 2017 GreenAddress
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bitcoinj.params;

/**
 * Parameters for elements.
 */
public class ElementsNetParams extends TestNet3Params {

    private static ElementsNetParams mInstance;

    public ElementsNetParams() {
        super();
        id = ID_ELEMENTS;
        packetMagic = 0xefb11fea;
        port = 9042;
        addressHeader = 235;
        p2shHeader = 40;
        acceptableAddressCodes = new int[] { addressHeader, p2shHeader };
    }

    public static synchronized ElementsNetParams get() {
        if (mInstance == null)
            mInstance = new ElementsNetParams();
        return mInstance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_ELEMENTS;
    }
}
