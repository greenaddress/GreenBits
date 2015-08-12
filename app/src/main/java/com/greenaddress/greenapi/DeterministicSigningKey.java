package com.greenaddress.greenapi;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

import java.util.List;

public class DeterministicSigningKey implements ISigningWallet {
    private DeterministicKey hdWallet;

    public DeterministicSigningKey(final DeterministicKey masterPrivateKey) {
        hdWallet = masterPrivateKey;
    }

    @Override
    public ISigningWallet deriveChildKey(final ChildNumber childNumber) {
        return new DeterministicSigningKey(HDKeyDerivation
            .deriveChildKey(hdWallet, childNumber));
    }

    @Override
    public ListenableFuture<byte[]> getIdentifier() {
        return Futures.immediateFuture(hdWallet.getIdentifier());
    }

    @Override
    public ListenableFuture<ECKey.ECDSASignature> signHash(
        final Sha256Hash hash) {
        return Futures.immediateFuture(ECKey
            .fromPrivate(hdWallet.getPrivKey()).sign(hash));
    }

    @Override
    public ListenableFuture<ECKey.ECDSASignature> signMessage(
        final String message) {
        return Futures.immediateFuture(null);
    }

    @Override
    public boolean canSignHashes() {
        return true;
    }

    @Override
    public ListenableFuture<DeterministicKey> getPubKey() {
        return Futures.immediateFuture(hdWallet);
    }

    @Override
    public ListenableFuture<List<ECKey.ECDSASignature>> signTransaction(
        final PreparedTransaction tx, final String coinName,
        final byte[] gait_path) {
        return null;
    }
}
