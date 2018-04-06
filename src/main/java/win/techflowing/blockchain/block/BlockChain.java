package win.techflowing.blockchain.block;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import win.techflowing.blockchain.transaction.SpendableOutputResult;
import win.techflowing.blockchain.transaction.TransInput;
import win.techflowing.blockchain.transaction.TransOutput;
import win.techflowing.blockchain.transaction.Transaction;
import win.techflowing.blockchain.utils.RocksDBUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 区块链
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class BlockChain {

    /** 最新一个区块的Hash值 */
    private String mLastBlockHash;

    public BlockChain(){

    }

    public BlockChain(String lastBlockHash) {
        this.mLastBlockHash = lastBlockHash;
    }

    /**
     * 添加区块
     *
     * @param block
     */
    private void addBlock(Block block) throws Exception {
        RocksDBUtils.getInstance().putLastBlockHash(block.getHash());
        RocksDBUtils.getInstance().putBlock(block);
        mLastBlockHash = block.getHash();
    }

    /**
     * 寻找能够花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @param amount     花费金额
     */
    public SpendableOutputResult findSpendableOutputs(byte[] pubKeyHash, int amount) throws Exception {
        Transaction[] unspentTrans = findUnspentTransactions(pubKeyHash);
        int accumulated = 0;
        Map<String, int[]> unspentOuts = new HashMap<>();
        for (Transaction transaction : unspentTrans) {

            String txId = Hex.encodeHexString(transaction.getTransHash());

            for (int outId = 0; outId < transaction.getOutputs().length; outId++) {

                TransOutput TransOutput = transaction.getOutputs()[outId];

                if (TransOutput.isLockedWithKey(pubKeyHash) && accumulated < amount) {
                    accumulated += TransOutput.getValue();

                    int[] outIds = unspentOuts.get(txId);
                    if (outIds == null) {
                        outIds = new int[]{outId};
                    } else {
                        outIds = ArrayUtils.add(outIds, outId);
                    }
                    unspentOuts.put(txId, outIds);
                    if (accumulated >= amount) {
                        break;
                    }
                }
            }
        }
        return new SpendableOutputResult(accumulated, unspentOuts);
    }

    /**
     * 查找钱包地址对应的所有UTXO
     *
     * @param pubKeyHash 钱包公钥Hash
     * @return
     */
    public TransOutput[] findUTXO(byte[] pubKeyHash) throws Exception {
        Transaction[] unspentTrans = this.findUnspentTransactions(pubKeyHash);
        TransOutput[] TransOutputs = {};
        if (unspentTrans == null || unspentTrans.length == 0) {
            return TransOutputs;
        }
        for (Transaction tx : unspentTrans) {
            for (TransOutput TransOutput : tx.getOutputs()) {
                if (TransOutput.isLockedWithKey(pubKeyHash)) {
                    TransOutputs = ArrayUtils.add(TransOutputs, TransOutput);
                }
            }
        }
        return TransOutputs;
    }


    /**
     * 查找钱包地址对应的所有未花费的交易
     *
     * @param pubKeyHash 钱包公钥Hash
     * @return
     */
    private Transaction[] findUnspentTransactions(byte[] pubKeyHash) throws Exception {
        Map<String, int[]> allSpentTransOutput = getAllSpentTransOutput(pubKeyHash);
        Transaction[] unspentTxs = {};

        // 再次遍历所有区块中的交易输出
        for (BlockChainIterator blockChainIterator = getBlockChainIterator(); blockChainIterator.hashNext(); ) {
            Block block = blockChainIterator.next();
            for (Transaction transaction : block.getTransactions()) {

                String transHash = Hex.encodeHexString(transaction.getTransHash());

                int[] spentOutIndexArray = allSpentTransOutput.get(transHash);

                for (int outIndex = 0; outIndex < transaction.getOutputs().length; outIndex++) {
                    if (spentOutIndexArray != null && ArrayUtils.contains(spentOutIndexArray, outIndex)) {
                        continue;
                    }

                    // 保存不存在 allSpentTransOutput 中的交易
                    if (transaction.getOutputs()[outIndex].isLockedWithKey(pubKeyHash)) {
                        unspentTxs = ArrayUtils.add(unspentTxs, transaction);
                    }
                }
            }
        }
        return unspentTxs;
    }

    /**
     * 从交易输入中查询区块链中所有已被花费了的交易输出
     *
     * @param pubKeyHash 钱包公钥Hash
     * @return 交易ID以及对应的交易输出下标地址
     * @throws Exception
     */
    private Map<String, int[]> getAllSpentTransOutput(byte[] pubKeyHash) throws Exception {
        // 定义TxId ——> spentOutIndex[]，存储交易ID与已被花费的交易输出数组索引值
        Map<String, int[]> spentTransOutput = new HashMap<>();
        for (BlockChainIterator blockchainIterator = getBlockChainIterator(); blockchainIterator.hashNext(); ) {
            Block block = blockchainIterator.next();

            for (Transaction transaction : block.getTransactions()) {
                // 如果是 coinbase 交易，直接跳过，因为它不存在引用前一个区块的交易输出
                if (transaction.isCoinbase()) {
                    continue;
                }
                for (TransInput TransInput : transaction.getInputs()) {
                    if (TransInput.usesKey(pubKeyHash)) {
                        String inputTransId = Hex.encodeHexString(TransInput.getTransId());
                        int[] spentOutIndexArray = spentTransOutput.get(inputTransId);
                        if (spentOutIndexArray == null) {
                            spentTransOutput.put(inputTransId, new int[]{TransInput.getTranceOutputIndex()});
                        } else {
                            spentOutIndexArray = ArrayUtils.add(spentOutIndexArray, TransInput.getTranceOutputIndex());
                            spentTransOutput.put(inputTransId, spentOutIndexArray);
                        }
                    }
                }
            }
        }
        return spentTransOutput;
    }

    /**
     * 进行交易签名
     *
     * @param transaction 交易数据
     * @param privateKey  私钥
     */
    public void signTransaction(Transaction transaction, BCECPrivateKey privateKey) throws Exception {
        // 先来找到这笔新的交易中，交易输入所引用的前面的多笔交易的数据
        Map<String, Transaction> prevTxMap = new HashMap<>();
        for (TransInput TransInput : transaction.getInputs()) {
            Transaction prevTx = findTransaction(TransInput.getTransId());
            prevTxMap.put(Hex.encodeHexString(TransInput.getTransId()), prevTx);
        }
        transaction.sign(privateKey, prevTxMap);
    }

    /**
     * 交易签名验证
     *
     * @param tx
     */
    private boolean verifyTransactions(Transaction tx) throws Exception {
        Map<String, Transaction> prevTx = new HashMap<>();
        for (TransInput transInput : tx.getInputs()) {
            Transaction transaction = findTransaction(transInput.getTransId());
            prevTx.put(Hex.encodeHexString(transInput.getTransId()), transaction);
        }
        try {
            return tx.verify(prevTx);
        } catch (Exception e) {
            throw new Exception("Fail to verify transaction ! transaction invalid ! ");
        }
    }

    /**
     * 依据交易ID查询交易信息
     *
     * @param txId 交易ID
     * @return
     */
    private Transaction findTransaction(byte[] txId) throws Exception {
        for (BlockChainIterator iterator = getBlockChainIterator(); iterator.hashNext(); ) {
            Block block = iterator.next();
            for (Transaction transaction : block.getTransactions()) {
                if (Arrays.equals(transaction.getTransHash(), txId)) {
                    return transaction;
                }
            }
        }
        throw new Exception("ERROR: Can not found tx by txId ! ");
    }


    /**
     * 打包交易，进行挖矿
     *
     * @param transactions
     */
    public void mineBlock(Transaction[] transactions) throws Exception {
        // 挖矿前，先验证交易记录
        for (Transaction tx : transactions) {
            if (!this.verifyTransactions(tx)) {
                throw new Exception("ERROR: Fail to mine block ! Invalid transaction ! ");
            }
        }
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to get last block hash ! ");
        }

        Block block = BlockChainUtils.createBlock(lastBlockHash, transactions);
        this.addBlock(block);
    }


    public BlockChainIterator getBlockChainIterator() {
        return new BlockChainIterator(mLastBlockHash);
    }

    /**
     * 区块链迭代器
     */
    public class BlockChainIterator {

        private String mCurrentBlockHash;

        private BlockChainIterator(String currentBlockHash) {
            this.mCurrentBlockHash = currentBlockHash;
        }

        /**
         * 是否有下一个区块
         *
         * @return
         */
        public boolean hashNext() throws Exception {
            if (StringUtils.isBlank(mCurrentBlockHash)) {
                return false;
            }
            Block lastBlock = RocksDBUtils.getInstance().getBlock(mCurrentBlockHash);
            if (lastBlock == null) {
                return false;
            }
            // 创世区块直接放行
            if (lastBlock.getPrevBlockHash().length() == 0) {
                return true;
            }
            return RocksDBUtils.getInstance().getBlock(lastBlock.getPrevBlockHash()) != null;
        }


        /**
         * 返回区块
         *
         * @return
         */
        public Block next() throws Exception {
            Block currentBlock = RocksDBUtils.getInstance().getBlock(mCurrentBlockHash);
            if (currentBlock != null) {
                this.mCurrentBlockHash = currentBlock.getPrevBlockHash();
                return currentBlock;
            }
            return null;
        }
    }
}
