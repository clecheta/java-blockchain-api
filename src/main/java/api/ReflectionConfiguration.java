package api;

import com.R07r35cInc.blockchain.*;
import com.R07r35cInc.cryptocurrency.Input;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.TransactionPool;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Calculate;
import com.R07r35cInc.util.CryptoHash;
import com.R07r35cInc.util.ECDSA;
import com.R07r35cInc.util.Parse;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@RegisterForReflection(targets = {
        //blockchain
        Block.class,
        BlockChain.class,
        Chain.class,
        CryptoChain.class,
        Miner.class,
        NetChain.class,
        //cryptocurrency
        Input.class,
        Transaction.class,
        TransactionPool.class,
        Wallet.class,
        //util
        Calculate.class,
        CryptoHash.class,
        ECDSA.class,
        Parse.class
})
public class ReflectionConfiguration {
    //TODO after a success on this issue, remove the unnecessary classes from this config and also the jandex plugin from pom.
}
