package com.greenaddress.greenapi;

import org.bitcoinj.core.NetworkParameters;

// FIXME: Needs updating when an environment is available
public abstract class Network {
    public final static NetworkParameters NETWORK = NetworkParameters.fromID(NetworkParameters.ID_ELEMENTS);
    public final static String GAIT_WAMP_URL = "wss://elementswss.greenaddress.it/v2/ws";
    public final static String[] GAIT_WAMP_CERT_PINS = null;
    public final static String BLOCKEXPLORER_ADDRESS = "https://test-insight.bitpay.com/address/";
    public final static String BLOCKEXPLORER_TX = "https://test-insight.bitpay.com/tx/";
    public final static String depositPubkey = "036307e560072ed6ce0aa5465534fb5c258a2ccfbc257f369e8e7a181b16d897b3";
    public final static String depositChainCode = "b60befcc619bb1c212732770fe181f2f1aa824ab89f8aab49f2e13e3a56f0f04";
    public final static String GAIT_ONION = null;
    public final static String DEFAULT_PEER = "";
}
