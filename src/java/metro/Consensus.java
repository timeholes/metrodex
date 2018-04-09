package metro;

import metro.crypto.HashFunction;
import metro.util.BitcoinJUtils;

import java.math.BigInteger;

public class Consensus {
    public static final HashFunction HASH_FUNCTION = HashFunction.SHA3;

    public static final short GENESIS_BLOCK_VERSION = 0;

    public static final int DIFFICULTY_TRANSITION_INTERVAL = 2016;
    public static final int TARGET_TIMESPAN = 1209600;

    public static final long MAX_WORK_BITS = Constants.isTestnet ? Long.parseUnsignedLong(Metro.getStringProperty("metro.testnetMaxWorkTarget", "1e00ffff"),16) : 0x1f000fffL;
    public static final BigInteger MAX_WORK_TARGET = BitcoinJUtils.decodeCompactBits(MAX_WORK_BITS);
    public static final int SUBSIDY_HALVING_INTERVAL = 210000;
    public static final long INITIAL_SUBSIDY = 2000 * Constants.ONE_MTR;

    public static final int GUARANTEED_BALANCE_KEYBLOCK_CONFIRMATIONS = 30;

    public static short getPosBlockVersion(int atHeight) {
        return 3;
    }

    public static short getKeyBlockVersion(int atHeight) {
        return (short)0x8001;
    }

    public static int getTransactionVersion(int atHeight) {
        return 1;
    }

    public static long getBlockSubsidy(int atLocalHeight) {
        int halvings = atLocalHeight / SUBSIDY_HALVING_INTERVAL;
        if (halvings >= 64)
            return 0;
        // Subsidy is cut in half every 210,000 blocks which will occur approximately every 4 years.
        return INITIAL_SUBSIDY >> halvings;
    }
}
