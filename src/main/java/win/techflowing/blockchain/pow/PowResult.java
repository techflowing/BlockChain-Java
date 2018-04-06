package win.techflowing.blockchain.pow;

/**
 * 工作量计算结果
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class PowResult {
    /** 计数器 */
    private long mNonce;
    /** hash值 */
    private String mHash;

    public PowResult() {

    }

    public PowResult(long nonce, String hash) {
        mNonce = nonce;
        mHash = hash;
    }


    public long getNonce() {
        return mNonce;
    }

    public String getHash() {
        return mHash;
    }
}
