package com.greenaddress.greenapi;

import com.blockstream.libwally.Wally;
import com.google.common.collect.ImmutableList;
import com.greenaddress.greenbits.ui.BuildConfig;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.LazyECPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.blockstream.libwally.Wally.BIP32_FLAG_KEY_PUBLIC;
import static com.blockstream.libwally.Wally.BIP32_VER_MAIN_PRIVATE;
import static com.blockstream.libwally.Wally.BIP32_VER_MAIN_PUBLIC;
import static com.blockstream.libwally.Wally.BIP32_VER_TEST_PRIVATE;
import static com.blockstream.libwally.Wally.BIP32_VER_TEST_PUBLIC;

public class HDKey {
    private final static int VER_PUBLIC = isMain() ? BIP32_VER_MAIN_PUBLIC : BIP32_VER_TEST_PUBLIC;
    private final static int VER_PRIVATE = isMain() ? BIP32_VER_MAIN_PRIVATE : BIP32_VER_TEST_PRIVATE;

    public static final int BRANCH_REGULAR = 1;

    private static final Map<Integer, DeterministicKey> mServerKeys = new HashMap<>();
    private static int[] mGaUserPath = null;

    private static boolean isMain() {
        return NetworkParameters.fromID(NetworkParameters.ID_MAINNET).equals(Network.NETWORK);
    }

    //
    // Temporary methods for use while converting from DeterministicKey
    public static DeterministicKey deriveChildKey(final DeterministicKey parent, final Integer childNum) {
        return HDKeyDerivation.deriveChildKey(parent, new ChildNumber(childNum));
    }

    public static DeterministicKey createMasterKeyFromSeed(final byte[] seed) {
        return HDKeyDerivation.createMasterPrivateKey(seed);
    }

    public static DeterministicKey createMasterKey(final byte[] chainCode, final byte[] publicKey) {
        final ECKey pub = ECKey.fromPublicOnly(publicKey);
        return new DeterministicKey(new ImmutableList.Builder<ChildNumber>().build(),
                                    chainCode, pub.getPubKeyPoint(), null, null);
    }

    public static DeterministicKey createMasterKey(final String chainCode, final String publicKey) {
        return createMasterKey(h(chainCode), h(publicKey));
    }

    // Get the 2of3 backup key (plus parent)
    // This is the users key to reedeem 2of3 funds in the event that GA becomes unavailable
    public static DeterministicKey[] getRecoveryKeys(final byte[] chainCode, final byte[] publicKey, final Integer pointer) {
        DeterministicKey[] ret = new DeterministicKey[2];
        ret[0] = deriveChildKey(createMasterKey(chainCode, publicKey), 1); // Parent
        ret[1] = deriveChildKey(ret[0], pointer); // Child
        return ret;
    }

    public static DeterministicKey[] getRecoveryKeys(final String chainCode, final String publicKey, final Integer pointer) {
        return getRecoveryKeys(h(chainCode), h(publicKey), pointer);
    }

    // Get the key derived from the servers public key/chaincode plus the users path (plus parent).
    // This is the key used on the servers side of 2of2/2of3 transactions.
    public static DeterministicKey[] getGAPublicKeys(final int subAccount, final Integer pointer) {
        DeterministicKey[] ret = new DeterministicKey[2];
        synchronized (mServerKeys) {
            // Fetch the parent key. This is expensive so we cache it
            if (!mServerKeys.containsKey(subAccount))
                mServerKeys.put(subAccount, ret[0] = getServerKeyImpl(subAccount));
            else
                ret[0] = mServerKeys.get(subAccount);
        }
        // Compute the child key if we were asked for it
        if (pointer != null)
            ret[1] = deriveChildKey(ret[0], pointer); // Child
        return ret;
    }

    public static void resetCache(final int[] gaUserPath) {
        synchronized (mServerKeys) {
            mServerKeys.clear();
            mGaUserPath = gaUserPath == null ? null : gaUserPath.clone();
        }
    }

    private static DeterministicKey getServerKeyImpl(final int subAccount) {
        final boolean reconcile = BuildConfig.DEBUG;
        DeterministicKey k = null;
        if (reconcile) {
            k = createMasterKey(Network.depositChainCode, Network.depositPubkey);
            k = deriveChildKey(k, subAccount == 0 ? 1 : 3);
            for (int i : mGaUserPath)
                k = deriveChildKey(k, i);
            if (subAccount != 0)
                k = deriveChildKey(k, subAccount);
        }

        Object master = Wally.bip32_key_init(VER_PUBLIC, 0, 0,
                                             h(Network.depositChainCode), h(Network.depositPubkey),
                                             null, null, null);
        final int[] path = new int[mGaUserPath.length + (subAccount == 0 ? 1 : 2)];
        path[0] = subAccount == 0 ? 1 : 3;
        System.arraycopy(mGaUserPath, 0, path, 1, mGaUserPath.length);
        if (subAccount != 0)
            path[mGaUserPath.length + 1] = subAccount;

        Object derived = Wally.bip32_key_from_parent_path(master, path, BIP32_FLAG_KEY_PUBLIC);

        final DeterministicKey key;
        final ArrayList<ChildNumber> childNumbers = new ArrayList<>(path.length);
        for (int i : path)
            childNumbers.add(new ChildNumber(i));
        key = new DeterministicKey(ImmutableList.<ChildNumber>builder().addAll(childNumbers).build(),
                                   Wally.bip32_key_get_chain_code(derived),
                                   new LazyECPoint(ECKey.CURVE.getCurve(),
                                   Wally.bip32_key_get_pub_key(derived)),
                                   /* parent */ null, childNumbers.size(), 0);

        if (reconcile)
            if (!k.equals(key))
                throw new RuntimeException("Derivation mismatch");

        Wally.bip32_key_free(master);
        Wally.bip32_key_free(derived);
        return key;
    }
    // FIXME: Remove
    private static byte[] h(final String hex) { return Wally.hex_to_bytes(hex); }
}
