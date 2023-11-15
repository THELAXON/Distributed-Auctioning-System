import java.io.Serializable;

public class AuctionResult implements Serializable {
    //private static final long serialVersionUID = 1L;

    private int auctionID;
    private int highestBid;

    public AuctionResult(int auctionID, int highestBid) {
        this.auctionID = auctionID;
        this.highestBid = highestBid;
    }

    public int getAuctionID() {
        return auctionID;
    }

    public int getHighestBid() {
        return highestBid;
    }
}
