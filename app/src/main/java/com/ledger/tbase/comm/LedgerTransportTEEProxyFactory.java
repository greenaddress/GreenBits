package com.ledger.tbase.comm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.btchip.comm.BTChipTransport;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.ledger.wallet.service.ILedgerWalletService;

public class LedgerTransportTEEProxyFactory implements BTChipTransportFactory {
	
	private Context context;
	private ILedgerWalletService service;
	private LedgerTransportTEEProxy transport;
	private BTChipTransportFactoryCallback callback;
	private ServiceConnection serviceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder serviceBinder) {
			Log.d(TAG, "Service connected");
			service = ILedgerWalletService.Stub.asInterface(serviceBinder);
			if (transport != null) {
				transport.setService(service);
			}
			if (callback != null) {
				BTChipTransportFactoryCallback currentCallback = callback;
				callback = null;
				currentCallback.onConnected(true);				
			}			
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "Service disconnected");
			if (callback != null) {
				BTChipTransportFactoryCallback currentCallback = callback;
				callback = null;
				currentCallback.onConnected(false);								
			}
			if (transport != null) {
				transport.setService(null);
			}
		}		
	};
	
	public static final String TAG="LedgerTransportTEEProxyFactory";

	public LedgerTransportTEEProxyFactory(Context context) {
		this.context = context;
	}
	
	@Override
	public BTChipTransport getTransport() {
		if (transport == null) {
			transport = new LedgerTransportTEEProxy(context);
		}
		return transport;
	}

	@Override
	public boolean isPluggedIn() {
		return true;
	}

	@Override
	public boolean connect(final Context context, final BTChipTransportFactoryCallback callback) {
		if (service != null) {
			callback.onConnected(true);
			return true;
		}
		this.callback = callback;
		Log.d(TAG, "Before bind service");
		try {
			Intent intent = new Intent(ILedgerWalletService.class.getName());
			intent.setPackage("com.ledger.wallet.service");
			boolean result = context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
			Log.d(TAG, "Request to bind service " + result);
			return result;
		}
		catch(Exception e) {
			Log.d(TAG, "Error binding service", e);
			return false;
		}		
	}

}
