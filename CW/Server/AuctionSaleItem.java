import java.io.Serializable;

public class AuctionSaleItem implements Serializable {
    //private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private int startingPrice;

    public AuctionSaleItem(String name, String description, int startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getStartingPrice() {
        return startingPrice;
    }
}
