package win.techflowing.blockchain.block;

import org.apache.commons.codec.digest.DigestUtils;
import win.techflowing.blockchain.transaction.Transaction;
import win.techflowing.blockchain.utils.ByteUtils;

/**
 * 区块信息实体
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class Block {
    /** 区块Hash值 */
    private String mHash;
    /** 前一个区块的hash值 */
    private String mPrevBlockHash;
    /** 区块数据 */
    private Transaction[] mTransactions;
    /** 区块创建时间 */
    private long mTimeStamp;
    /** 工作量证明计数器 */
    private long mNonce;

    public Block() {

    }

    public Block(String hash, String preBlockHash, Transaction[] transactions, long timeStamp, long nonce) {
        mHash = hash;
        mPrevBlockHash = preBlockHash;
        mTransactions = transactions;
        mTimeStamp = timeStamp;
        mNonce = nonce;
    }

    public String getHash() {
        return mHash;
    }

    public void setHash(String hash) {
        mHash = hash;
    }

    public String getPrevBlockHash() {
        return mPrevBlockHash;
    }

    public void setPrevBlockHash(String prevBlockHash) {
        mPrevBlockHash = prevBlockHash;
    }

    public Transaction[] getTransactions() {
        return mTransactions;
    }

    public void setTransactions(Transaction[] transactions) {
        this.mTransactions = transactions;
    }

    public long getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        mTimeStamp = timeStamp;
    }

    public long getNonce() {
        return mNonce;
    }

    public void setNonce(long nonce) {
        mNonce = nonce;
    }


    /**
     * 对区块中的交易信息进行Hash计算
     *
     * @return
     */
    public byte[] hashTransaction() {
        byte[][] txIdArrays = new byte[this.getTransactions().length][];
        for (int i = 0; i < this.getTransactions().length; i++) {
            txIdArrays[i] = this.getTransactions()[i].hash();
        }
        return DigestUtils.sha256(ByteUtils.merge(txIdArrays));
    }
}
