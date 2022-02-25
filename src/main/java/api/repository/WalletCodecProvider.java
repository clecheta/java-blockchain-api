package api.repository;

import com.R07r35cInc.cryptocurrency.Wallet;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public class WalletCodecProvider implements CodecProvider {
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz.equals(Wallet.class)) {
            return (Codec<T>) new WalletCodec();
        }
        return null;
    }
}
