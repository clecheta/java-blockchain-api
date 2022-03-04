package api.repository;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.ECDSA;
import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class WalletCodec implements CollectibleCodec<Wallet> {

    private static final Logger LOGGER = Logger.getLogger("WalletCodec");
    private final Codec<Document> documentCodec;

    public WalletCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Wallet wallet, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("balance", Base64.getEncoder().encodeToString(wallet.balance.unscaledValue().toByteArray()));
        doc.put("uid", wallet.uid);
        doc.put("publicKey", Base64.getEncoder().encodeToString(wallet.keyPair.getPublic().getEncoded()));
        doc.put("privateKey", Base64.getEncoder().encodeToString(wallet.keyPair.getPrivate().getEncoded()));
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
        byte[] encodedBalance = Base64.getDecoder().decode(document.getString("balance"));
        long valueBalance = new BigInteger(encodedBalance).longValue();
        BigDecimal balance = new BigDecimal(valueBalance);

        ECPublicKey publicKey = ECDSA.loadPublicKey(document.getString("publicKey"));
        ECPrivateKey privateKey = ECDSA.loadPrivateKey(document.getString("privateKey"));
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return new Wallet(keyPair, balance, uid);
    }

}
