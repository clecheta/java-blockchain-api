package api;

import com.R07r35cInc.blockchain.*;
import com.R07r35cInc.cryptocurrency.Input;
import com.R07r35cInc.cryptocurrency.PayTypeTransaction;
import com.R07r35cInc.cryptocurrency.Transaction;
import com.R07r35cInc.cryptocurrency.Tx;
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
        NetChain.class,
        //cryptocurrency
        Input.class,
        Tx.class,
        Transaction.class,
        PayTypeTransaction.class
})
public class ReflectionConfiguration {

}
