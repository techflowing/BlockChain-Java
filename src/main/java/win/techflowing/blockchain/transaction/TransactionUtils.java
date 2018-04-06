package win.techflowing.blockchain.transaction;

import org.apache.commons.lang3.StringUtils;
import win.techflowing.blockchain.utils.Base58Check;

import java.util.Arrays;

/**
 * 交易工具类
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/6
 */
public class TransactionUtils {

    private static final int SUBSIDY = 10;

    /**
     * 创建交易输出
     *
     * @param value
     * @param address
     * @return
     */
    public static TransOutput createTransOutput(int value, String address) {
        // 反向转化为 byte 数组
        byte[] versionedPayload = Base58Check.base58ToBytes(address);
        byte[] pubKeyHash = Arrays.copyOfRange(versionedPayload, 1, versionedPayload.length);
        return new TransOutput(value, pubKeyHash);
    }

    /**
     * 创建CoinBase交易
     *
     * @param to   收账的钱包地址
     * @param data 解锁脚本数据
     * @return
     */
    public static Transaction createCoinbaseTrans(String to, String data) {
        if (StringUtils.isBlank(data)) {
            data = String.format("Reward to '%s'", to);
        }
        // 创建交易输入
        TransInput TransInput = new TransInput(new byte[]{}, -1, null, data.getBytes());
        // 创建交易输出
        TransOutput TransOutput = createTransOutput(SUBSIDY, to);
        // 创建交易
        Transaction transaction = new Transaction(null, new TransInput[]{TransInput}, new TransOutput[]{TransOutput});
        // 设置交易ID
        transaction.setTransHash(transaction.hash());
        return transaction;
    }
}
