package api.repository;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.MongoClientSettings;
import org.bson.*;
import org.bson.codecs.Codec;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class BlockCodec implements CollectibleCodec<Block> {

    private final Codec<Document> documentCodec;

    public BlockCodec() {
        this.documentCodec = MongoClientSettings.getDefaultCodecRegistry().get(Document.class);
    }

    @Override
    public void encode(BsonWriter writer, Block block, EncoderContext encoderContext) {
        Document doc = new Document();
        doc.put("date", block.getDate());
        doc.put("previousHash", block.getPreviousHash());
        doc.put("hash", block.getHash());
        doc.put("data", Parse.toJson(block.getData()));
        doc.put("nonce", block.getNonce());
        doc.put("index", block.getIndex());
        doc.put("chain", block.getChain());
        documentCodec.encode(writer, doc, encoderContext);
    }

    @Override
    public Class<Block> getEncoderClass() {
        return Block.class;
    }

    @Override
    public Block generateIdIfAbsentFromDocument(Block document) {
        if (!documentHasId(document)) {
            throw new RuntimeException("The block is missing hash: there is no such a thing");
        }
        return document;
    }

    @Override
    public boolean documentHasId(Block document) {
        return document.getHash() != null;
    }

    @Override
    public BsonValue getDocumentId(Block document) {
        return new BsonString(document.getHash());
    }

    @Override
    public Block decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        Block block = new Block();
        block.setDate(document.getDate("date"));
        block.setPreviousHash(document.getString("previousHash"));
        block.setHash(document.getString("hash"));

        Parse<List<Transaction>> parse = new Parse<>();
        String dataString = document.getString("data");
        Object data = parse.fromJson(dataString, new TypeReference<List<Transaction>>() {});
        block.setData(data);

        block.setNonce(document.getLong("nonce"));
        block.setIndex(document.getLong("index"));
        block.setChain(document.getString("chain"));
        return block;
    }

}
