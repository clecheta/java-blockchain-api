package api.repository;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class WalletCodec implements CollectibleCodec<Wallet> {

    private static final Logger LOGGER = Logger.getLogger("WalletCodec");

    private final Codec<Document> documentCodec;
    private static final Parse<Wallet> jsonParse = new Parse<>();

    public WalletCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Wallet wallet, EncoderContext encoderContext) {
        Document doc = new Document();
        //doc.put("keyPair", wallet.getKeyPair().getPrivate().getEncoded());
//        doc.put("balance", wallet.balance);
//        doc.put("publicKey", wallet.publicKey);
        doc.put("uid", wallet.uid);
        doc.put("json", Parse.toJson(wallet));
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
        String json = document.getString("json");
        Wallet wallet = jsonParse.fromJson(json, new TypeReference<Wallet>() {});
//        Decimal128 dec = document.get
//        wallet.balance = new BigDecimal(document.getString("balance"));
//        wallet.publicKey = document.getString("publicKey");

        return wallet;
    }

}
