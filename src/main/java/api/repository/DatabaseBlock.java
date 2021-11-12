package api.repository;

import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.util.Parse;
import io.quarkus.mongodb.panache.common.MongoEntity;

import java.util.Date;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@MongoEntity(database = "chain", collection = "block")
public class DatabaseBlock {
    public Date date;
    public String previousHash;
    public String hash;
    public String data;
    public Long nonce;
    public Long index;

    public String chain;

    public DatabaseBlock() {

    }

    public DatabaseBlock(Block block, String chain) {
        this.date = block.getDate();
        this.previousHash = block.getPreviousHash();
        this.hash = block.getHash();
        this.data = Parse.toJson(block.getData());
        this.nonce = block.getNonce();
        this.index = block.getIndex();
        this.chain = chain;
    }

}
