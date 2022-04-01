package api.cryptocurrency;

import api.cryptocurrency.model.WalletInfo;
import com.R07r35cInc.util.Calculate;
import com.R07r35cInc.util.Parse;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("CrYP70")
public class CryptoResource {

    @Inject
    TransactionMiner miner;

    @POST
    @Produces("application/json")
    @Path("1Nf0")
    public Response info() {
        String address = miner.wallet.publicKey;
        WalletInfo walletInfo = new WalletInfo(address,
                Calculate.walletBalance(miner.cryptoChain.getChain(), address, miner.wallet.getSTARTING_BALANCE()));
        return Response.ok(walletInfo).build();
    }

    @POST
    @Produces("application/json")
    @Path("Ch41N")
    public Response blocks() {
        String jsonChain = Parse.toJson(miner.cryptoChain.getChain());
        return Response.ok(jsonChain).build();
    }

}