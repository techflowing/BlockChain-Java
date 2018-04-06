package win.techflowing.blockchain.utils;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import win.techflowing.blockchain.block.Block;

/**
 * RocksDB（数据持久化） 工具类
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class RocksDBUtils {
    /** 区块链数据文件 */
    private static final String DB_FILE = "blockchain.db";
    /** 区块桶前缀 */
    private static final String BLOCKS_BUCKET_PREFIX = "blocks_";
    /** 最新一个区块hash的键值对 flag */
    private static final String LAST_BLOCK_FLAG = "l";
    /** 单例 */
    private volatile static RocksDBUtils sInstance;
    /** 数据库持久化 */
    private RocksDB mRocksDB;

    /**
     * 获取单例
     *
     * @return 单例
     */
    public static RocksDBUtils getInstance() {
        if (sInstance == null) {
            synchronized (RocksDBUtils.class) {
                if (sInstance == null) {
                    sInstance = new RocksDBUtils();
                }
            }
        }
        return sInstance;
    }

    /**
     * 构造函数
     */
    private RocksDBUtils() {
        initRocksDB();
    }

    /**
     * 初始化RocksDB
     */
    private void initRocksDB() {
        try {
            mRocksDB = RocksDB.open(new Options().setCreateIfMissing(true), DB_FILE);
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存最新一个区块的Hash值
     *
     * @param tipBlockHash
     */
    public void putLastBlockHash(String tipBlockHash) throws Exception {
        mRocksDB.put(SerializeUtils.serialize(BLOCKS_BUCKET_PREFIX + LAST_BLOCK_FLAG),
                SerializeUtils.serialize(tipBlockHash));
    }

    /**
     * 查询最新一个区块的Hash值
     *
     * @return
     */
    public String getLastBlockHash() throws Exception {
        byte[] lastBlockHashBytes = mRocksDB.get(SerializeUtils.serialize(BLOCKS_BUCKET_PREFIX + LAST_BLOCK_FLAG));
        if (lastBlockHashBytes != null) {
            return (String) SerializeUtils.deserialize(lastBlockHashBytes);
        }
        return "";
    }

    /**
     * 保存区块
     *
     * @param block
     */
    public void putBlock(Block block) throws Exception {
        byte[] key = SerializeUtils.serialize(BLOCKS_BUCKET_PREFIX + block.getHash());
        mRocksDB.put(key, SerializeUtils.serialize(block));
    }

    /**
     * 查询区块
     *
     * @param blockHash
     * @return
     */
    public Block getBlock(String blockHash) throws Exception {
        byte[] key = SerializeUtils.serialize(BLOCKS_BUCKET_PREFIX + blockHash);
        return (Block) SerializeUtils.deserialize(mRocksDB.get(key));
    }

    /**
     * 关闭数据库
     */
    public void closeDB() {
        if (mRocksDB != null) {
            mRocksDB.close();
        }
    }
}
