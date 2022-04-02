package api.repository;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.ECDSA;
import com.R07r35cInc.util.Parse;
import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
        doc.put("c3", Base64.getEncoder().encodeToString(wallet.balance.unscaledValue().toByteArray())); //src
        doc.put("a0", wallet.uid); //origin
        doc.put("d7", Base64.getEncoder().encodeToString(wallet.publicKey.getBytes(StandardCharsets.UTF_8)));
        doc.put("b8", Base64.getEncoder().encodeToString(wallet.getSTARTING_BALANCE().unscaledValue().toByteArray()));
        doc.put("e5", Parse.encodeHexString(wallet.keyPair.getPrivate().getEncoded()));
        doc.put("j1", Base64.getEncoder().encodeToString(Parse.longToBytes(wallet.nonce)));
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

        byte[] encodedBalance = Base64.getDecoder().decode(document.getString("c3"));
        long valueBalance = new BigInteger(encodedBalance).longValue();
        BigDecimal balance = new BigDecimal(valueBalance);

        String uid = document.getString("a0");

        byte[] encodedPublicKey = Base64.getDecoder().decode(document.getString("d7"));
        String stringPublicKey = new String(encodedPublicKey, StandardCharsets.UTF_8);
        ECPublicKey publicKey = ECDSA.loadPublicKey(stringPublicKey);

        byte[] encodedStartBalance = Base64.getDecoder().decode(document.getString("b8"));
        long valueStartBalance = new BigInteger(encodedStartBalance).longValue();
        BigDecimal startBalance = new BigDecimal(valueStartBalance);

        byte[] privateKeyByteArray = Parse.decodeHexString(document.getString("e5"));
        ECPrivateKey privateKey = ECDSA.loadPrivateKey(privateKeyByteArray);

        byte[] encodedNonce = Base64.getDecoder().decode(document.getString("j1"));
        Long nonce = Parse.bytesToLong(encodedNonce);

        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        return new Wallet(keyPair, balance, uid, startBalance, nonce);
    }

}
