package win.techflowing.blockchain.block;

import org.apache.commons.lang3.StringUtils;
import win.techflowing.blockchain.pow.PowResult;
import win.techflowing.blockchain.pow.ProofOfWork;
import win.techflowing.blockchain.pow.ProofUtils;
import win.techflowing.blockchain.transaction.Transaction;
import win.techflowing.blockchain.transaction.TransactionUtils;
import win.techflowing.blockchain.utils.RocksDBUtils;

import java.time.Instant;

/**
 * 区块链工具
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class BlockChainUtils {


    /**
     * 从 DB 从恢复区块链数据
     *
     * @return
     * @throws Exception
     */
    public static BlockChain initBlockchainFromDB() throws Exception {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (lastBlockHash == null) {
            throw new Exception("ERROR: Fail to init blockchain from db. ");
        }
        return new BlockChain(lastBlockHash);
    }

    /**
     * 创建创世区块
     *
     * @param transaction 交易信息
     */
    public static Block createGenesisBlock(Transaction transaction) {
        return createBlock("", new Transaction[]{transaction});
    }

    /**
     * 创建新区块
     *
     * @param previousHash 前一个块的hash值
     * @param transactions 交易信息
     * @return
     */
    public static Block createBlock(String previousHash, Transaction[] transactions) {
        Block block = new Block("", previousHash, transactions, Instant.now().getEpochSecond(), 0);
        ProofOfWork pow = ProofUtils.createProofOfWork(block);
        PowResult powResult = pow.run();
        block.setHash(powResult.getHash());
        block.setNonce(powResult.getNonce());
        return block;
    }

    /**
     * 创建区块链
     *
     * @param address 钱包地址
     * @return
     */
    public static BlockChain createBlockChain(String address) throws Exception {
        String lastBlockHash = RocksDBUtils.getInstance().getLastBlockHash();
        if (StringUtils.isBlank(lastBlockHash)) {
            // 创建 coinBase 交易
            String genesisCoinbaseData = "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks";
            Transaction coinbaseTrans = TransactionUtils.createCoinbaseTrans(address, genesisCoinbaseData);
            Block genesisBlock = BlockChainUtils.createGenesisBlock(coinbaseTrans);
            lastBlockHash = genesisBlock.getHash();
            RocksDBUtils.getInstance().putBlock(genesisBlock);
            RocksDBUtils.getInstance().putLastBlockHash(lastBlockHash);
        }
        return new BlockChain(lastBlockHash);
    }

}
