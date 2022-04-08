package api.cryptocurrency;

import api.cryptocurrency.model.WalletInfo;
import api.repository.WalletRepository;
import com.R07r35cInc.cryptocurrency.Wallet;
import org.apache.http.HttpStatus;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
@Path("W4LL37")
public class WalletResource {

    @Inject
    WalletRepository repository;

    @POST
    @Produces("application/json")
    @Path("cRE47E")
    public Response create(String userIdentification) {
        Wallet newWallet;
        if (userIdentification == null || userIdentification.length()==0) {
            newWallet = new Wallet();
        } else {
            if (userIdentification.length() < 20) {
                return Response.status(HttpStatus.SC_BAD_REQUEST, "too short identification").build();
            }
            newWallet = new Wallet(userIdentification);
        }
        String originalId = newWallet.uid;

        try {
            repository.add(newWallet);
        } catch (RuntimeException re) {
            return Response.status(HttpStatus.SC_BAD_REQUEST).build(); //do not warn about database reasons
        }

        WalletInfo walletInfo = new WalletInfo(newWallet.publicKey, newWallet.balance);
        walletInfo.uid = originalId;
        return Response.ok(walletInfo).build();
    }

    @POST
    @Produces("application/json")
    @Path("937")
    public Response get(String userIdentification) {
        //Never get wallet from publicAddress, this information must be kept in the backend
        try {
            Wallet existingWallet = repository.getByUid(userIdentification);
            return Response.ok(new WalletInfo(existingWallet.publicKey, existingWallet.balance)).build();
        } catch (RuntimeException re) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

}
