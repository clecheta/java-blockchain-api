package api.repository;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.ECDSA;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class WalletCodec implements CollectibleCodec<Wallet> {

    // Can't find a codec for class sun.security.ec.ECPrivateKeyImpl.

    private final com.R07r35cInc.util.ECDSA ECDSA; //utility class to work with ECDSA algorithms
    private static final Logger LOGGER = Logger.getLogger("WalletCodec");

    private final Codec<Document> documentCodec;
    private static final Parse<PrivateKey> privKeyParse = new Parse<>();
    private static final Parse<PublicKey> pubKeyParse = new Parse<>();

    public WalletCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
        ECDSA = new ECDSA();
    }

    @Override
    public void encode(BsonWriter writer, Wallet wallet, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("balance", wallet.balance);
        doc.put("uid", wallet.uid);
        ECDSA.setKeyPair(wallet.keyPair);
        ECPrivateKey ecPrivkey = (ECPrivateKey) wallet.keyPair.getPrivate();
        doc.put("privateKey", ecPrivkey.getEncoded());
        ECPublicKey ecPubkey = (ECPublicKey) wallet.keyPair.getPublic();
        doc.put("publicKey", ecPubkey.getEncoded());
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<Wallet> getEncoderClass() {
        return Wallet.class;
    }

    @Override
    public Wallet generateIdIfAbsentFromDocument(Wallet document) {
        if (!documentHasId(document)) {
            throw new RuntimeException("The wallet is missing uid - please help me out");
        }
        return document;
    }

    @Override
    public boolean documentHasId(Wallet document) {
        return document.uid != null;
    }

    @Override
    public BsonValue getDocumentId(Wallet document) {
        return new BsonString(document.uid);
    }

    @Override
    public Wallet decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        String uid = document.getString("uid");
        String privateKeyEncoded = document.getString("privateKey");
        String publicKeyEncoded = document.getString("publicKey");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyEncoded);

        /*String privateKeyBase64 = document.getString("privateKey");
        String publicKeyBase64 = document.getString("publicKey");
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);
        KeyPair keyPair;
        try {
            KeyFactory kf = KeyFactory.getInstance("EC");
            X509EncodedKeySpec X509privateKey = new X509EncodedKeySpec(privateKeyBytes);
            PrivateKey privateKey = kf.generatePrivate(X509privateKey);
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey publicKey = kf.generatePublic(X509publicKey);
            keyPair = new KeyPair(publicKey, privateKey);
            ECDSA.setKeyPair(keyPair);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }*/

//        Decimal128 dec = document.get
//        wallet.balance = new BigDecimal(document.getString("balance"));
//        wallet.publicKey = document.getString("publicKey");
        return new Wallet();
    }

}
