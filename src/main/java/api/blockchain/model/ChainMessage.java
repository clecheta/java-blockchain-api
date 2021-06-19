package api.blockchain.model;

import com.R07r35cInc.Block;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/* Parsed objects through network channels must have all attributes made public
   or they must have the getters and setters */
@RegisterForReflection
public class ChainMessage {

    public String from;
    public List<Block> chain;

}
