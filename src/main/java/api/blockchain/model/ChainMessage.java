package api.blockchain.model;

import com.R07r35cInc.Block;

import java.util.ArrayList;
import java.util.List;

public class ChainMessage {

    private List<Block> blockList = new ArrayList<Block>();

    public ChainMessage() {
    }

    public ChainMessage(List<Block> blockList) {
        this.blockList = blockList;
    }

    public List<Block> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<Block> blockList) {
        this.blockList = blockList;
    }

}
