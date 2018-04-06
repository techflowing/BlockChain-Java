package win.techflowing.blockchain.pow;

import win.techflowing.blockchain.block.Block;

import java.math.BigInteger;


/**
 * 工作量工具类
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class ProofUtils {
    /**
     * 创建新的工作量证明，设定难度目标值
     * <p>
     * 对1进行移位运算，将1向左移动 (256 - TARGET_BITS) 位，得到我们的难度目标值
     *
     * @param block 区块
     */
    public static ProofOfWork createProofOfWork(Block block) {
        BigInteger targetValue = BigInteger.valueOf(1).shiftLeft((256 - ProofOfWork.TARGET_BITS));
        return new ProofOfWork(block, targetValue);
    }
}
