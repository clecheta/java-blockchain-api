package api.cryptocurrency;

import api.cryptocurrency.model.WalletInfo;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Wallet;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("W4LL37")
public class WalletResource {

    @Inject
    TransactionMiner miner;

    @Inject
    WalletRepository repository;

    @POST
    @Produces("application/json")
    @Path("1Nf0")
    public Response info() {
        String address = miner.wallet.publicKey;
        WalletInfo walletInfo = new WalletInfo();
        walletInfo.address = address;
        walletInfo.balance = Wallet.calculateBalance(miner.cryptoChain.getChain(), address);
        return Response.ok(walletInfo).build();
    }

    @POST
    @Produces("application/json")
    @Path("cRE47E")
    public Response create() {
        Wallet newWallet = new Wallet();
        repository.add(newWallet);
        return Response.ok(new WalletInfo(newWallet.publicKey, newWallet.balance, newWallet.uid)).build();
    }

    @POST
    @Produces("application/json")
    @Path("937")
    public Response get(String id) {
        Wallet existingWallet = repository.get(id);
        return Response.ok(new WalletInfo(existingWallet.publicKey, existingWallet.balance)).build();
    }
}
