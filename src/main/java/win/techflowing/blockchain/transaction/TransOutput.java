package win.techflowing.blockchain.transaction;

import java.util.Arrays;

/**
 * 交易输出
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class TransOutput {
    /** 数值,储着 satoshis 的任意倍的数值 */
    private int mValue;
    /** 公钥Hash */
    private byte[] mPubKeyHash;


    public TransOutput() {
    }

    public TransOutput(int value, byte[] pubKeyHash) {
        mValue = value;
        mPubKeyHash = pubKeyHash;
    }

    public int getValue() {
        return mValue;
    }

    public byte[] getPubKeyHash() {
        return mPubKeyHash;
    }

    /**
     * 检查交易输出是否能够使用指定的公钥
     *
     * @param pubKeyHash
     * @return
     */
    public boolean isLockedWithKey(byte[] pubKeyHash) {
        return Arrays.equals(getPubKeyHash(), pubKeyHash);
    }
}
