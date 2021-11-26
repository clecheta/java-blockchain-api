package api.repository;

import com.R07r35cInc.blockchain.Block;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class BlockCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Block.class)) {
            return (Codec<T>) new BlockCodec();
        }
        return null;
    }
}
