package api.repository.entity;

import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.Tx;
import com.R07r35cInc.util.Parse;
import com.fasterxml.jackson.core.type.TypeReference;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "ledger_block")
public class BlockEntity extends PanacheEntityBase {

    private static final Parse<List<Transaction>> CRYPTO_PARSE = new Parse<>();
    private static final Parse<Object> OBJECT_PARSE = new Parse<>();

    @Id
    @Column(length = 128)
    public String hash;

    @Column(name = "chain_type", nullable = false, length = 32)
    public String chainType;

    @Column(name = "block_number", nullable = false)
    public Long blockNumber;

    @Column(name = "previous_hash", nullable = false, length = 128)
    public String previousHash;

    @Column(nullable = false)
    public Long nonce;

    @Column(name = "mined_at", nullable = false)
    public Instant minedAt;

    @Column(name = "data_json", columnDefinition = "text")
    public String dataJson;

    public static BlockEntity fromDomain(Block block, ChainTypeEnum type) {
        BlockEntity entity = new BlockEntity();
        entity.hash = block.getHash();
        entity.chainType = type.getValue();
        entity.blockNumber = block.getNumber();
        entity.previousHash = block.getPreviousHash();
        entity.nonce = block.getNonce();
        entity.minedAt = block.getDate().toInstant();
        entity.dataJson = Parse.toJson(block.getData());
        return entity;
    }

    public Block toDomain() {
        Block block = new Block();
        block.setHash(hash);
        block.setChain(chainType);
        block.setNumber(blockNumber);
        block.setPreviousHash(previousHash);
        block.setNonce(nonce);
        block.setDate(Date.from(minedAt));
        Object data = ChainTypeEnum.CRYPTO.getValue().equals(chainType)
                ? CRYPTO_PARSE.fromJson(dataJson, new TypeReference<List<Tx>>() {})
                : OBJECT_PARSE.fromJson(dataJson, new TypeReference<>() {});
        block.setData(data);
        return block;
    }
}
