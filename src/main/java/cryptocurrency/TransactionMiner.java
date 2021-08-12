package cryptocurrency;

import api.blockchain.SharedService;

import javax.inject.Inject;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public class TransactionMiner {

    @Inject
    SharedService sharedService;

    /** All necessary functionality to add a block of transaction poll data to the blockchain */
    void mineTransaction() {
        //1. get the transaction pool's valid transactions

        //2. generate the miner's reward

        //3. add a block consisting of these transactions to the blockchain

        //4. broadcast the updated blockchain

        //5. clear the pool
    }

}
