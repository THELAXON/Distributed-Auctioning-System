import java.io.Serializable;

public class AuctionItem implements Serializable {
    // private static final long serialVersionUID = 1L;

    private int itemID;
    private String name;
    private String description;
    private int highestBid;

    public AuctionItem(int itemID, String name, String description, int highestBid) {
        this.itemID = itemID;
        this.name = name;
        this.description = description;
        this.highestBid = highestBid;
    }

    public int getItemID() {
        return itemID;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(int highestBid) {
        this.highestBid = highestBid;
    }
}
