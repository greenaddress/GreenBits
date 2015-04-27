/*
*******************************************************************************    
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.btchip;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.ExecutionException;

import com.btchip.comm.BTChipTransport;
import com.btchip.utils.BIP32Utils;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.CoinFormatUtils;
import com.btchip.utils.Dump;
import com.btchip.utils.VarintUtils;

public class BTChipDongle implements BTChipConstants {
	
	public enum OperationMode {
		WALLET(0x01),
		RELAXED_WALLET(0x02),
		SERVER(0x04),
		DEVELOPER(0x08);
		
		private int value;
		
		OperationMode(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	};
	
	public enum Feature {
		UNCOMPRESSED_KEYS(0x01),
		RFC6979(0x02),
		FREE_SIGHASHTYPE(0x04),
		NO_2FA_P2SH(0x08);
		
		private int value;
		
		Feature(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	};
	
	public class BTChipPublicKey {
		private byte[] publicKey;
		private String address;
		private byte[] chainCode;
		
		public BTChipPublicKey(byte[] publicKey, String address, byte[] chainCode) {
			this.publicKey = publicKey;
			this.address = address;
			this.chainCode = chainCode;
		}
		
		public byte[] getPublicKey() {
			return publicKey;
		}		
		public String getAddress() {
			return address;
		}		
		public byte[] getChainCode() {
			return chainCode;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Address ");
			buffer.append(address);
			buffer.append(" public key ");
			buffer.append(Dump.dump(publicKey));
			buffer.append(" chaincode ");
			buffer.append(Dump.dump(chainCode));
			return buffer.toString();
		}
	}
	
	public class BTChipSignature {
		private byte[] signature;
		private int yParity;
		
		public BTChipSignature(byte[] signature, int yParity) {
			this.signature = signature;
			this.yParity = yParity;
		}
		
		public byte[] getSignature() {
			return signature;
		}		
		public int getYParity() {
			return yParity;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Signature ");
			buffer.append(Dump.dump(signature));
			buffer.append(" y parity ");
			buffer.append(yParity);
			return buffer.toString();
		}
	}
	
	class BTChipFirmware {
		private int major;
		private int minor;
		private int patch;
		private boolean compressedKeys;
		
		public BTChipFirmware(int major, int minor, int patch, boolean compressedKeys) {
			this.major = major;
			this.minor = minor;
			this.patch = patch;
			this.compressedKeys = compressedKeys;
		}
		
		public int getMajor() {
			return major;
		}
		public int getMinor() {
			return minor;
		}
		public int getPatch() {
			return patch;
		}
		public boolean isCompressedKey() {
			return compressedKeys;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append(major).append('.').append(minor).append('.').append(patch);
			buffer.append(" compressed keys ");
			buffer.append(compressedKeys);
			return buffer.toString();
		}
	}
	
	public class BTChipInput {
		private byte[] value;
		private boolean trusted;
		
		public BTChipInput(byte[] value, boolean trusted) {
			this.value = value;
			this.trusted = trusted;
		}
		
		public byte[] getValue() {
			return value;
		}
		public boolean isTrusted() {
			return trusted;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Value ").append(Dump.dump(value));
			buffer.append(" trusted ").append(trusted);
			return buffer.toString();
		}
	}

	public class BTChipOutput {
		private byte[] value;
		private boolean confirmationNeeded;
		
		public BTChipOutput(byte[] value, boolean confirmationNeeded) {
			this.value = value;
			this.confirmationNeeded = confirmationNeeded;
		}
		
		public byte[] getValue() {
			return value;
		}
		public boolean isConfirmationNeeded() {
			return confirmationNeeded;
		}
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Value ").append(Dump.dump(value));
			buffer.append(" confirmation needed ").append(confirmationNeeded);
			return buffer.toString();
		}
	}	
	
	private BTChipTransport transport;
	private int lastSW;
	private boolean supportScreen;
	
	private static final int OK[] = { SW_OK };
	private static final byte DUMMY[] = { 0 };
	
	public BTChipDongle(BTChipTransport transport) {
		this.transport = transport;
	}
	
	public BTChipDongle(BTChipTransport transport, boolean supportScreen) {
		this.transport = transport;
		this.supportScreen = supportScreen;
	}	
	
	public boolean hasScreenSupport() {
		return supportScreen;
	}
		
	private byte[] exchange(byte[] apdu) throws BTChipException {
		byte[] response;
		try {
			response = transport.exchange(apdu).get();
		}
		catch(Exception e) {
			throw new BTChipException("I/O error", e);
		}
		if (response.length < 2) {
			throw new BTChipException("Truncated response");
		}
		lastSW = ((int)(response[response.length - 2] & 0xff) << 8) | 
				(int)(response[response.length - 1] & 0xff);
		byte[] result = new byte[response.length - 2];
		System.arraycopy(response, 0, result, 0, response.length - 2);
		return result;
	}
	
	private byte[] exchangeCheck(byte[] apdu, int acceptedSW[]) throws BTChipException {
		byte[] response = exchange(apdu);
		if (acceptedSW == null) {
			return response;
		}
		for (int SW : acceptedSW) {
			if (lastSW == SW) {
				return response;
			}
		}
		throw new BTChipException("Invalid status", lastSW);
	}
	
	private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
		byte[] apdu = new byte[data.length + 5];
		apdu[0] = cla;
		apdu[1] = ins;
		apdu[2] = p1;
		apdu[3] = p2;
		apdu[4] = (byte)(data.length);
		System.arraycopy(data, 0, apdu, 5, data.length);
		return exchangeCheck(apdu, acceptedSW);
	}
	
	private byte[] exchangeApdu(byte cla, byte ins, byte p1, byte p2, int length, int acceptedSW[]) throws BTChipException {
		byte[] apdu = new byte[5];
		apdu[0] = cla;
		apdu[1] = ins;
		apdu[2] = p1;
		apdu[3] = p2;
		apdu[4] = (byte)(length);
		return exchangeCheck(apdu, acceptedSW);
	}

	private byte[] exchangeApduSplit(byte cla, byte ins, byte p1, byte p2, byte[] data, int acceptedSW[]) throws BTChipException {
		int offset = 0;
		byte[] result = null;
		while (offset < data.length) {
			int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
			byte[] apdu = new byte[blockLength + 5];
			apdu[0] = cla;
			apdu[1] = ins;
			apdu[2] = p1;
			apdu[3] = p2;
			apdu[4] = (byte)(blockLength);
			System.arraycopy(data, offset, apdu, 5, blockLength);
			result = exchangeCheck(apdu, acceptedSW);
			offset += blockLength;
		}
		return result;
	}
	
	public void verifyPin(byte[] pin) throws BTChipException {
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_VERIFY_PIN, (byte)0x00, (byte)0x00, pin, OK);
	}
	
	public int getVerifyPinRemainingAttempts() throws BTChipException {
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_VERIFY_PIN, (byte)0x80, (byte)0x00, DUMMY, null);
		if ((lastSW & 0xfff0) != 0x63c0) {
			throw new BTChipException("Invalid status", lastSW);
		}
		return (lastSW - 0x63c0);
	}
	
	public BTChipPublicKey getWalletPublicKey(String keyPath) throws BTChipException {
		byte data[] = BIP32Utils.splitPath(keyPath);
		byte response[] = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_WALLET_PUBLIC_KEY, (byte)0x00, (byte)0x00, data, OK);
		int offset = 0;
		byte publicKey[] = new byte[response[offset]];
		offset++;
		System.arraycopy(response, offset, publicKey, 0, publicKey.length);
		offset += publicKey.length;
		byte address[] = new byte[response[offset]];
		offset++;
		System.arraycopy(response, offset, address, 0, address.length);
		offset += address.length;
		byte chainCode[] = new byte[32];
		System.arraycopy(response, offset, chainCode, 0, chainCode.length);
		offset += address.length;		
		return new BTChipPublicKey(publicKey, new String(address), chainCode);
	}
	
	public BTChipInput getTrustedInput(BitcoinTransaction transaction, long index) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Header
		BufferUtils.writeUint32BE(data, index);
		BufferUtils.writeBuffer(data, transaction.getVersion());
		VarintUtils.write(data, transaction.getInputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		// Each input
		for (BitcoinTransaction.BitcoinInput input : transaction.getInputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getPrevOut());
			VarintUtils.write(data, input.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getScript());
			BufferUtils.writeBuffer(data, input.getSequence());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);			
		}
		// Number of outputs
		data = new ByteArrayOutputStream();
		VarintUtils.write(data, transaction.getOutputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		// Each output
		for (BitcoinTransaction.BitcoinOutput output : transaction.getOutputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getAmount());
			VarintUtils.write(data, output.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getScript());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);						
		}
		// Locktime
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, transaction.getLockTime(), OK);		
		return new BTChipInput(response, true);
	}

	public BTChipInput createInput(byte[] value, boolean trusted) {
        	return new BTChipInput(value, trusted);
    	}
	
	public void startUntrustedTransction(boolean newTransaction, long inputIndex, BTChipInput usedInputList[], byte[] redeemScript) throws BTChipException {
		// Start building a fake transaction with the passed inputs
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(data, BitcoinTransaction.DEFAULT_VERSION);
		VarintUtils.write(data, usedInputList.length);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x00, (newTransaction ? (byte)0x00 : (byte)0x80), data.toByteArray(), OK);
		// Loop for each input
		long currentIndex = 0;
		for (BTChipInput input : usedInputList) {
			byte[] script = (currentIndex == inputIndex ? redeemScript : new byte[0]);
			data = new ByteArrayOutputStream();
			data.write(input.isTrusted() ? (byte)0x01 : (byte)0x00);
			if (input.isTrusted()) {
				data.write(input.getValue().length);
			}
			BufferUtils.writeBuffer(data, input.getValue());
			VarintUtils.write(data, script.length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, script);
			BufferUtils.writeBuffer(data, BitcoinTransaction.DEFAULT_SEQUENCE);
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			currentIndex++;			
		}				
	}
	
	public BTChipOutput finalizeInput(String outputAddress, String amount, String fees, String changePath) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = BIP32Utils.splitPath(changePath);
		data.write(outputAddress.length());
		BufferUtils.writeBuffer(data, outputAddress.getBytes());
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(amount));
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(fees));
		BufferUtils.writeBuffer(data, path);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE, (byte)0x02, (byte)0x00, data.toByteArray(), OK);
		byte[] value = new byte[(int)(response[0] & 0xff)];
		System.arraycopy(response, 1, value, 0, value.length);
		boolean confirmationNeeded = (response[1 + value.length] == (byte)0x01);
		return new BTChipOutput(value, confirmationNeeded);
	}
	
	public boolean finalizeInputFull(byte[] data) throws BTChipException {
		int offset = 0;
		byte[] result = null;
		while (offset < data.length) {
			int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
			byte[] apdu = new byte[blockLength + 5];
			apdu[0] = BTCHIP_CLA;
			apdu[1] = BTCHIP_INS_HASH_INPUT_FINALIZE_FULL;
			apdu[2] = ((offset + blockLength) == data.length ? (byte)0x80 : (byte)0x00);
			apdu[3] = (byte)0x00;
			apdu[4] = (byte)(blockLength);
			System.arraycopy(data, offset, apdu, 5, blockLength);
			result = exchangeCheck(apdu, OK);
			offset += blockLength;
		}
		return (result[0] == (byte)0x01);
	}
	
	public byte[] untrustedHashSign(String privateKeyPath, String pin, long lockTime, byte sigHashType) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = BIP32Utils.splitPath(privateKeyPath);
		BufferUtils.writeBuffer(data, path);
		data.write(pin.length());
		BufferUtils.writeBuffer(data, pin.getBytes());
		BufferUtils.writeUint32BE(data, lockTime);
		data.write(sigHashType);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_SIGN, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		response[0] = (byte)0x30;
		return response;
	}
	
	public byte[] untrustedHashSign(String privateKeyPath, String pin) throws BTChipException {
		return untrustedHashSign(privateKeyPath, pin, 0, (byte)0x01);
	}
	
	public boolean signMessagePrepare(String path, byte[] message) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(data, BIP32Utils.splitPath(path));
		data.write((byte)message.length);
		BufferUtils.writeBuffer(data, message);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SIGN_MESSAGE, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		return (response[0] == (byte)0x01);
	}
	
	public BTChipSignature signMessageSign(byte[] pin) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		if (pin == null) {
			data.write((byte)0);
		}
		else {
			data.write((byte)pin.length);
			BufferUtils.writeBuffer(data, pin);
		}
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SIGN_MESSAGE, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		int yParity = (response[0] & 0x0F);
		response[0] = (byte)0x30;
		return new BTChipSignature(response, yParity);
	}
	
	public BTChipFirmware getFirmwareVersion() throws BTChipException {
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_FIRMWARE_VERSION, (byte)0x00, (byte)0x00, 0x00, OK);
		boolean compressedKeys = (response[0] == (byte)0x01);
		int major = ((int)(response[1] & 0xff) << 8) | ((int)(response[2] & 0xff));
		int minor = (int)(response[3] & 0xff);
		int patch = (int)(response[4] & 0xff);
		return new BTChipFirmware(major, minor, patch, compressedKeys);
	}		
	
	public void setKeymapEncoding(byte[] keymapEncoding) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		//data.write(keymapEncoding.length);
		BufferUtils.writeBuffer(data, keymapEncoding);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SET_KEYMAP, (byte)0x00, (byte)0x00, data.toByteArray(), OK);		
	}
	
	public boolean setup(OperationMode supportedOperationModes[], Feature features[], int keyVersion, int keyVersionP2SH, byte[] userPin, byte[] wipePin, byte[] keymapEncoding, byte[] seed, byte[] developerKey) throws BTChipException {
		int operationModeFlags = 0;
		int featuresFlags = 0;
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		for (OperationMode currentOperationMode : supportedOperationModes) {
			operationModeFlags |= currentOperationMode.getValue();
		}
		for (Feature currentFeature : features) {
			featuresFlags |= currentFeature.getValue();
		}
		data.write(operationModeFlags);
		data.write(featuresFlags);
		data.write(keyVersion);
		data.write(keyVersionP2SH);
		if ((userPin.length < 0x04) || (userPin.length > 0x20)) {
			throw new BTChipException("Invalid user PIN length");
		}
		data.write(userPin.length);
		BufferUtils.writeBuffer(data,  userPin);
		if (wipePin != null) {
			if (wipePin.length > 0x04) {
				throw new BTChipException("Invalid wipe PIN length");	
			}
			data.write(wipePin.length);
			BufferUtils.writeBuffer(data,  wipePin);			
		}
		else {
			data.write(0);
		}
		if (seed != null) {
			if ((seed.length < 32) || (seed.length > 64)) {
				throw new BTChipException("Invalid seed length");
			}
			data.write(seed.length);
			BufferUtils.writeBuffer(data, seed);
		}
		else {
			data.write(0);
		}
		if (developerKey != null) {
			if (developerKey.length != 0x10) {
				throw new BTChipException("Invalid developer key");
			}
			data.write(developerKey.length);
			BufferUtils.writeBuffer(data, developerKey);
		}
		else {
			data.write(0);
		}
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_SETUP, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		if (keymapEncoding != null) {
			setKeymapEncoding(keymapEncoding);
		}
		return (response[0] == (byte)0x01);
	}
	
	public byte[] compressPublicKey(byte[] publicKey) {
		byte[] result = new byte[33];
		result[0] = (((publicKey[64] & 1) != 0) ? (byte)0x03 : (byte)0x02);
		System.arraycopy(publicKey, 1, result, 1, 32);
		return result;		
	}
}
