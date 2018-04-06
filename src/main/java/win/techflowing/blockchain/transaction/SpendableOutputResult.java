package win.techflowing.blockchain.transaction;

import java.util.Map;

/**
 * 查询结果
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/6
 */
public class SpendableOutputResult {
    /** 交易时的支付金额 */
    private int mAccumulated;
    /** 未花费的交易 */
    private Map<String, int[]> mUnspentOuts;


    public SpendableOutputResult(int accumulated, Map<String, int[]> unspentOuts) {
        mAccumulated = accumulated;
        mUnspentOuts = unspentOuts;
    }

    public int getAccumulated() {
        return mAccumulated;
    }

    public void setAccumulated(int accumulated) {
        mAccumulated = accumulated;
    }

    public Map<String, int[]> getUnspentOuts() {
        return mUnspentOuts;
    }

    public void setUnspentOuts(Map<String, int[]> unspentOuts) {
        mUnspentOuts = unspentOuts;
    }
}
