package api.repository.entity;

import com.R07r35cInc.cryptocurrency.Wallet;
import com.R07r35cInc.util.ECDSA;
import com.R07r35cInc.util.Parse;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.security.KeyPair;
import java.time.Instant;

@Entity
@Table(name = "ledger_wallet")
public class WalletEntity extends PanacheEntityBase {

    @Id
    @Column(name = "uid_hash", length = 64)
    public String uidHash;

    @Column(name = "public_key", nullable = false, unique = true, columnDefinition = "text")
    public String publicKey;

    @Column(name = "private_key", nullable = false, columnDefinition = "text")
    public String privateKey;

    @Column(nullable = false, columnDefinition = "numeric")
    public BigDecimal balance;

    @Column(name = "starting_balance", nullable = false, columnDefinition = "numeric")
    public BigDecimal startingBalance;

    @Column(nullable = false)
    public Long nonce;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;

    public static WalletEntity fromDomain(Wallet wallet) {
        WalletEntity entity = new WalletEntity();
        entity.uidHash = wallet.uid;
        entity.publicKey = wallet.publicKey;
        entity.privateKey = Parse.encodeHexString(wallet.keyPair.getPrivate().getEncoded());
        entity.balance = wallet.balance;
        entity.startingBalance = wallet.getSTARTING_BALANCE();
        entity.nonce = wallet.nonce;
        entity.createdAt = Instant.now();
        entity.updatedAt = entity.createdAt;
        return entity;
    }

    public Wallet toDomain() {
        KeyPair keyPair = new KeyPair(
                ECDSA.loadPublicKey(publicKey),
                ECDSA.loadPrivateKey(Parse.decodeHexString(privateKey)));
        return new Wallet(keyPair, balance, uidHash, startingBalance, nonce);
    }
}
