package api.blockchain.model;

import api.util.Network;

import java.util.List;

/**
 * @author Cleber Lecheta
 * https://encontreijesus.com
 */
public abstract class ChainMessage<T> {
    public String from;

    /**
     * Used to avoid redundant interactions on replace chain
     */
    public Boolean isValid() {
        String currentHost = Network.getHostName();
        Boolean isValid = false;
        if (this != null && this.from != null && getChain() != null) {
            isValid = !this.from.equals(currentHost);
        }
        return isValid;
    }

    public abstract List<T> getChain();
}
