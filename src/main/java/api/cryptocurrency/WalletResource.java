package api.cryptocurrency;

import api.cryptocurrency.model.WalletInfo;
import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.Parse;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
@Path("W4LL37")
public class WalletResource {

    @Inject
    TransactionMiner miner;

    @POST
    @Produces("application/json")
    @Path("1Nf0")
    public Response info() {
        String address = miner.wallet.publicKey;

        WalletInfo walletInfo = new WalletInfo();
        walletInfo.address = address;
        walletInfo.balance = Wallet.calculateBalance(miner.cryptoChain.getChain(), address);

        String response = Parse.toJson(walletInfo);
        System.out.println("WalletInfo: " + response);

        return Response.ok(response).build();
    }
}
