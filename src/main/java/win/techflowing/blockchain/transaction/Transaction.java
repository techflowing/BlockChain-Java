package win.techflowing.blockchain.transaction;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import win.techflowing.blockchain.Wallet.Wallet;
import win.techflowing.blockchain.Wallet.WalletUtils;
import win.techflowing.blockchain.block.BlockChain;
import win.techflowing.blockchain.utils.BtcAddressUtils;
import win.techflowing.blockchain.utils.SerializeUtils;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * 交易信息类
 *
 * @author techflowing
 * @version v1.0
 * @since 2018/4/5
 */
public class Transaction {

    /***/
    private static final int SUBSIDY = 10;
    /** 交易的Hash */
    private byte[] mTransHash;
    /** 交易输入 */
    private TransInput[] mInputs;
    /** 交易输出 */
    private TransOutput[] mOutputs;

    public Transaction() {

    }

    public Transaction(byte[] transHash, TransInput[] inputs, TransOutput[] outputs) {
        mTransHash = transHash;
        mInputs = inputs;
        mOutputs = outputs;
    }


    public void setTransHash(byte[] transHash) {
        mTransHash = transHash;
    }

    public void setInputs(TransInput[] inputs) {
        mInputs = inputs;
    }

    public void setOutputs(TransOutput[] outputs) {
        mOutputs = outputs;
    }

    public byte[] getTransHash() {
        return mTransHash;
    }

    public TransInput[] getInputs() {
        return mInputs;
    }

    public TransOutput[] getOutputs() {
        return mOutputs;
    }

    /**
     * 计算交易信息的Hash值
     *
     * @return
     */
    public byte[] hash() {
        // 使用序列化的方式对Transaction对象进行深度复制
        byte[] serializeBytes = SerializeUtils.serialize(this);
        Transaction copyTransaction = (Transaction) SerializeUtils.deserialize(serializeBytes);
        copyTransaction.setTransHash(new byte[]{});
        return DigestUtils.sha256(SerializeUtils.serialize(copyTransaction));
    }

    /**
     * 是否为 Coinbase 交易
     *
     * @return
     */
    public boolean isCoinbase() {
        return getInputs().length == 1
                && getInputs()[0].getTransId().length == 0
                && getInputs()[0].getTranceOutputIndex() == -1;
    }

    /**
     * 从 from 向  to 支付一定的 amount 的金额
     *
     * @param from       支付钱包地址
     * @param to         收款钱包地址
     * @param amount     交易金额
     * @param blockchain 区块链
     * @return
     */
    public static Transaction newUTXOTransaction(String from, String to, int amount, BlockChain blockchain) throws Exception {
        // 获取钱包
        Wallet senderWallet = WalletUtils.getInstance().getWallet(from);
        byte[] pubKey = senderWallet.getPublicKey();
        byte[] pubKeyHash = BtcAddressUtils.ripeMD160Hash(pubKey);

        SpendableOutputResult result = blockchain.findSpendableOutputs(pubKeyHash, amount);
        int accumulated = result.getAccumulated();
        Map<String, int[]> unspentOuts = result.getUnspentOuts();

        if (accumulated < amount) {
            throw new Exception("ERROR: Not enough funds ! ");
        }
        Iterator<Map.Entry<String, int[]>> iterator = unspentOuts.entrySet().iterator();

        TransInput[] TransInputs = {};
        while (iterator.hasNext()) {
            Map.Entry<String, int[]> entry = iterator.next();
            String txIdStr = entry.getKey();
            int[] outIds = entry.getValue();
            byte[] txId = Hex.decodeHex(txIdStr);
            for (int outIndex : outIds) {
                TransInputs = ArrayUtils.add(TransInputs, new TransInput(txId, outIndex, null, pubKey));
            }
        }

        TransOutput[] TransOutputs = {};
        TransOutputs = ArrayUtils.add(TransOutputs, TransactionUtils.createTransOutput(amount, to));
        if (accumulated > amount) {
            TransOutputs = ArrayUtils.add(TransOutputs, TransactionUtils.createTransOutput((accumulated - amount), from));
        }

        Transaction newTrans = new Transaction(null, TransInputs, TransOutputs);
        newTrans.setTransHash(newTrans.hash());

        // 进行交易签名
        blockchain.signTransaction(newTrans, senderWallet.getPrivateKey());

        return newTrans;
    }

    /**
     * 签名
     *
     * @param privateKey 私钥
     * @param prevTxMap  前面多笔交易集合
     */
    public void sign(BCECPrivateKey privateKey, Map<String, Transaction> prevTxMap) throws Exception {
        // coinbase 交易信息不需要签名，因为它不存在交易输入信息
        if (this.isCoinbase()) {
            return;
        }
        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TransInput TransInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(TransInput.getTransId())) == null) {
                throw new Exception("ERROR: Previous transaction is not correct");
            }
        }

        // 创建用于签名的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        Signature ecdsaSign = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);
        ecdsaSign.initSign(privateKey);

        for (int i = 0; i < txCopy.getInputs().length; i++) {
            TransInput transInputCopy = txCopy.getInputs()[i];
            // 获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(transInputCopy.getTransId()));
            // 获取交易输入所对应的上一笔交易中的交易输出
            TransOutput prevTransOutput = prevTx.getOutputs()[transInputCopy.getTranceOutputIndex()];
            transInputCopy.setPubKey(prevTransOutput.getPubKeyHash());
            transInputCopy.setSignature(null);
            // 得到要签名的数据，即交易ID
            txCopy.setTransHash(txCopy.hash());
            transInputCopy.setPubKey(null);

            // 对整个交易信息仅进行签名，即对交易ID进行签名
            ecdsaSign.update(txCopy.getTransHash());
            byte[] signature = ecdsaSign.sign();

            // 将整个交易数据的签名赋值给交易输入，因为交易输入需要包含整个交易信息的签名
            // 注意是将得到的签名赋值给原交易信息中的交易输入
            this.getInputs()[i].setSignature(signature);
        }
    }

    /**
     * 创建用于签名的交易数据副本，交易输入的 signature 和 pubKey 需要设置为null
     *
     * @return
     */
    public Transaction trimmedCopy() {
        TransInput[] tmpTransInputs = new TransInput[this.getInputs().length];
        for (int i = 0; i < this.getInputs().length; i++) {
            TransInput transInput = this.getInputs()[i];
            tmpTransInputs[i] = new TransInput(transInput.getTransId(), transInput.getTranceOutputIndex(), null, null);
        }

        TransOutput[] tmpTransOutputs = new TransOutput[this.getOutputs().length];
        for (int i = 0; i < this.getOutputs().length; i++) {
            TransOutput transOutput = this.getOutputs()[i];
            tmpTransOutputs[i] = new TransOutput(transOutput.getValue(), transOutput.getPubKeyHash());
        }

        return new Transaction(this.getTransHash(), tmpTransInputs, tmpTransOutputs);
    }


    /**
     * 验证交易信息
     *
     * @param prevTxMap 前面多笔交易集合
     * @return
     */
    public boolean verify(Map<String, Transaction> prevTxMap) throws Exception {
        // coinbase 交易信息不需要签名，也就无需验证
        if (this.isCoinbase()) {
            return true;
        }

        // 再次验证一下交易信息中的交易输入是否正确，也就是能否查找对应的交易数据
        for (TransInput transInput : this.getInputs()) {
            if (prevTxMap.get(Hex.encodeHexString(transInput.getTransId())) == null) {
                throw new Exception("ERROR: Previous transaction is not correct");
            }
        }

        // 创建用于签名验证的交易信息的副本
        Transaction txCopy = this.trimmedCopy();

        Security.addProvider(new BouncyCastleProvider());
        ECParameterSpec ecParameters = ECNamedCurveTable.getParameterSpec("secp256k1");
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA", BouncyCastleProvider.PROVIDER_NAME);
        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", BouncyCastleProvider.PROVIDER_NAME);

        for (int i = 0; i < this.getInputs().length; i++) {
            TransInput transInput = this.getInputs()[i];
            // 获取交易输入TxID对应的交易数据
            Transaction prevTx = prevTxMap.get(Hex.encodeHexString(transInput.getTransId()));
            // 获取交易输入所对应的上一笔交易中的交易输出
            TransOutput prevTransOutput = prevTx.getOutputs()[transInput.getTranceOutputIndex()];

            TransInput transInputCopy = txCopy.getInputs()[i];
            transInputCopy.setSignature(null);
            transInputCopy.setPubKey(prevTransOutput.getPubKeyHash());
            // 得到要签名的数据，即交易ID
            txCopy.setTransHash(txCopy.hash());
            transInputCopy.setPubKey(null);

            // 使用椭圆曲线 x,y 点去生成公钥Key
            BigInteger x = new BigInteger(1, Arrays.copyOfRange(transInput.getPubKey(), 1, 33));
            BigInteger y = new BigInteger(1, Arrays.copyOfRange(transInput.getPubKey(), 33, 65));
            ECPoint ecPoint = ecParameters.getCurve().createPoint(x, y);

            ECPublicKeySpec keySpec = new ECPublicKeySpec(ecPoint, ecParameters);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(txCopy.getTransHash());
            if (!ecdsaVerify.verify(transInput.getSignature())) {
                return false;
            }
        }
        return true;
    }
}
