package win.techflowing.blockchain.transaction;

import win.techflowing.blockchain.utils.BtcAddressUtils;

import java.util.Arrays;

/**
 * 交易输入
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class TransInput {
    /** 交易Id的hash值 */
    private byte[] mTransId;
    /** 交易输出索引,一笔交易可能包含多个交易输出 */
    private int mTranceOutputIndex;
    /** 签名*/
    private byte[] mSignature;
    /**公钥*/
    private byte[] mPubKey;

    public TransInput() {
    }

    public TransInput(byte[] transId, int tranceOutputIndex, byte[] signature, byte[] pubKey) {
        mTransId = transId;
        mTranceOutputIndex = tranceOutputIndex;
        mSignature = signature;
        mPubKey = pubKey;
    }

    public byte[] getTransId() {
        return mTransId;
    }

    public int getTranceOutputIndex() {
        return mTranceOutputIndex;
    }

    public byte[] getSignature() {
        return mSignature;
    }

    public byte[] getPubKey() {
        return mPubKey;
    }

    public void setTransId(byte[] transId) {
        mTransId = transId;
    }

    public void setTranceOutputIndex(int tranceOutputIndex) {
        mTranceOutputIndex = tranceOutputIndex;
    }

    public void setSignature(byte[] signature) {
        mSignature = signature;
    }

    public void setPubKey(byte[] pubKey) {
        mPubKey = pubKey;
    }

    /**
     * 检查公钥hash是否用于交易输入
     *
     * @param pubKeyHash
     * @return
     */
    public boolean usesKey(byte[] pubKeyHash) {
        byte[] lockingHash = BtcAddressUtils.ripeMD160Hash(getPubKey());
        return Arrays.equals(lockingHash, pubKeyHash);
    }

}
