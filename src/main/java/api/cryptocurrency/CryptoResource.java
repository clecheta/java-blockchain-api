package api.cryptocurrency;

import api.cryptocurrency.model.WalletInfo;
import api.repository.BlockRepository;
import com.R07r35cInc.util.Calculate;
import com.R07r35cInc.util.Parse;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("CrYP70")
public class CryptoResource {

    @Inject
    TransactionMiner miner;

    @Inject
    BlockRepository blockRepository;

    @POST
    @Produces("application/json")
    @Path("1Nf0")
    public Response info() {
        //TODO create a total chain transact money calculation with all the inputs wallets have received
        //or even sum all the wallets balance without faucet computation
        //even better make a job that compute this value on daily bases and save it to further read just the latest registry
        String address = miner.rewardWallet.publicKey;
        WalletInfo walletInfo = new WalletInfo(address,
                Calculate.walletBalance(miner.cryptoChain.getChain(), address, miner.rewardWallet.getSTARTING_BALANCE()));
        return Response.ok(walletInfo).build();
    }

    @POST
    @Produces("application/json")
    @Path("Ch41N")
    public Response blocks() {
        String jsonChain = Parse.toJson(miner.cryptoChain.getChain());
        return Response.ok(jsonChain).build();
    }

    @POST
    @Produces("application/json")
    @Path("Ch41N/from/{number}")
    public Response blocksFrom(@PathParam("number") long number) {
        String jsonChain = Parse.toJson(blockRepository.loadCryptochainFromNumber(number));
        return Response.ok(jsonChain).build();
    }

}