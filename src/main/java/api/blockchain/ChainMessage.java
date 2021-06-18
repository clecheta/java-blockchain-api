package api.blockchain;

import java.util.ArrayList;
import java.util.List;

public class ChainMessage {

    private List<String> blockList = new ArrayList<String>();

    public ChainMessage() {
    }

    public ChainMessage(List<String> blockList) {
        this.blockList = blockList;
    }

    public List<String> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<String> blockList) {
        this.blockList = blockList;
    }

}
