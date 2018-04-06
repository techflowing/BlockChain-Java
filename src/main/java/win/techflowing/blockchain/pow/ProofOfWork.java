package win.techflowing.blockchain.pow;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import win.techflowing.blockchain.block.Block;
import win.techflowing.blockchain.utils.ByteUtils;

import java.math.BigInteger;

/**
 * 工作量证明
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class ProofOfWork {

    /** 难度目标位 */
    public static final int TARGET_BITS = 20;
    /** 区块 */
    private Block mBlock;
    /** 难度目标值 */
    private BigInteger mTarget;

    public ProofOfWork() {

    }

    public ProofOfWork(Block block, BigInteger target) {
        mBlock = block;
        mTarget = target;
    }

    public Block getBlock() {
        return mBlock;
    }

    public BigInteger getTarget() {
        return mTarget;
    }

    /**
     * 运行工作量证明，开始挖矿，找到小于难度目标值的Hash。
     * <p>
     * 循环体里面主要以下四步：
     * 准备数据
     * 进行sha256运算
     * 转化为BigInter类型
     * 与target进行比较
     * 最后，返回正确的Hash值以及运算计数器nonce
     *
     * @return 挖矿结果
     */
    public PowResult run() {
        long nonce = 0;
        String shaHex = "";
        long startTime = System.currentTimeMillis();
        while (nonce < Long.MAX_VALUE) {
            byte[] data = this.prepareData(nonce);
            shaHex = DigestUtils.sha256Hex(data);
            if (new BigInteger(shaHex, 16).compareTo(mTarget) < 0) {
                System.out.printf("Elapsed Time: %s seconds \n", (float) (System.currentTimeMillis() - startTime) / 1000);
                System.out.printf("correct hash Hex: %s \n\n", shaHex);
                break;
            } else {
                nonce++;
            }
        }
        return new PowResult(nonce, shaHex);
    }


    /**
     * 准备数据
     * 参与Hash运算的如下几个信息：
     * 前一个区块（父区块）的Hash值
     * 区块中的交易数据
     * 区块生成的时间
     * 难度目标
     * 用于工作量证明算法的计数器
     *
     * @param nonce 计数器
     * @return
     */
    private byte[] prepareData(long nonce) {
        byte[] prevBlockHashBytes = {};
        if (StringUtils.isNoneBlank(getBlock().getPrevBlockHash())) {
            prevBlockHashBytes = new BigInteger(getBlock().getPrevBlockHash(), 16).toByteArray();
        }

        return ByteUtils.merge(
                prevBlockHashBytes,
                getBlock().hashTransaction(),
                ByteUtils.longToBytes(getBlock().getTimeStamp()),
                ByteUtils.longToBytes(TARGET_BITS),
                ByteUtils.longToBytes(nonce)
        );
    }

    /**
     * 验证区块是否有效
     *
     * @return
     */
    public boolean validate() {
        byte[] data = this.prepareData(this.getBlock().getNonce());
        return new BigInteger(DigestUtils.sha256Hex(data), 16).compareTo(mTarget) < 0;
    }
}
