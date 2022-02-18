package api.repository.model;

/**
 * @author Cleber Lecheta
 * https://encontreiJesus.com
 */
public enum ChainTypeEnum {
    GENERIC("blockchain"),
    CRYPTO("cryptochain");

    private final String value;

    ChainTypeEnum(String value) {
        this.value = value;
    }

    public String getKey() {
        return name();
    }

    public String getValue() {
        return this.value;
    }

    public String toString() {
        return this.value;
    }
}
