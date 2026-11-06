package api.repository;

import api.repository.entity.BlockEntity;
import api.repository.model.ChainTypeEnum;
import com.R07r35cInc.blockchain.Block;
import com.R07r35cInc.cryptocurrency.Tx;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@ApplicationScoped
public class BlockRepository {

    private static final Logger LOGGER = Logger.getLogger("BlockRepository");
    public List<Block> loadBlockchain() {
        List<Block> list = new ArrayList<>();
        list.add(0, Block.genesis()); //add the Genesis block to begin of chain
        List<BlockEntity> entities = BlockEntity.find(
                "chainType = ?1 order by blockNumber",
                ChainTypeEnum.GENERIC.getValue()).list();
        entities.forEach(entity -> list.add(entity.toDomain()));
        LOGGER.debug("load: Blockchain");
        return list;
    }

    public List<Block<List<Tx>>> loadCryptochain() {
        List<Block<List<Tx>>> list = new ArrayList<>();
        list.add(0, Block.genesis()); //add the Genesis block to begin of chain

        List<BlockEntity> entities = BlockEntity.find(
                "chainType = ?1 order by blockNumber",
                ChainTypeEnum.CRYPTO.getValue()).list();
        entities.forEach(entity -> list.add(entity.toDomain()));
        LOGGER.debug("load: Cryptochain");

        return list;
    }

    /**
     * Load generic blocks with number &gt;= fromInclusive (no genesis prepend).
     * Used by peer catch-up after a Kafka gap.
     */
    public List<Block> loadBlockchainFromNumber(long fromInclusive) {
        List<Block> list = new ArrayList<>();
        List<BlockEntity> entities = BlockEntity.find(
                "chainType = ?1 and blockNumber >= ?2 order by blockNumber",
                ChainTypeEnum.GENERIC.getValue(), fromInclusive).list();
        entities.forEach(entity -> list.add(entity.toDomain()));
        LOGGER.debugf("load: Blockchain from number %d (%d blocks)", fromInclusive, list.size());
        return list;
    }

    /**
     * Load crypto blocks with number &gt;= fromInclusive (no genesis prepend).
     */
    public List<Block<List<Tx>>> loadCryptochainFromNumber(long fromInclusive) {
        List<Block<List<Tx>>> list = new ArrayList<>();
        List<BlockEntity> entities = BlockEntity.find(
                "chainType = ?1 and blockNumber >= ?2 order by blockNumber",
                ChainTypeEnum.CRYPTO.getValue(), fromInclusive).list();
        entities.forEach(entity -> list.add(entity.toDomain()));
        LOGGER.debugf("load: Cryptochain from number %d (%d blocks)", fromInclusive, list.size());
        return list;
    }

    public Block<List<Tx>> loadCryptochainTip() {
        BlockEntity entity = BlockEntity.find(
                "chainType = ?1 order by blockNumber desc",
                ChainTypeEnum.CRYPTO.getValue()).firstResult();
        return entity == null ? Block.genesis() : entity.toDomain();
    }

    @Transactional
    public void add(Block block, ChainTypeEnum chainType) {
        block.setChain(chainType.getValue());
        BlockEntity.fromDomain(block, chainType).persistAndFlush();
    }

}
