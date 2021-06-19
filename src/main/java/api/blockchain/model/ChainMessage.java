package api.blockchain.model;

import api.blockchain.util.NetUtil;
import com.R07r35cInc.Block;

import java.util.ArrayList;
import java.util.List;

/* Parsed objects must have all attributes made public or having getters and setters */
public class ChainMessage {

    private String node;
    private List<Block> blockList;

    /* Classes that will be parsed/serialized must have an empty default construct,*/
    public ChainMessage() { }

    public ChainMessage(List<Block> blockList) {
        this.node = NetUtil.getHostName();
        this.blockList = blockList;
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<Block> blockList) {
        this.blockList = blockList;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

}
