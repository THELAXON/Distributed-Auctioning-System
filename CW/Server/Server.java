import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class Server implements Auction {
    private Map<Integer, AuctionItem> auctionItems = new HashMap<>();
    private Map<Integer, Integer> userIDs = new HashMap<>();
    private int auctionIDCounter = 1;

    public Server() {
        super();
        // Hardcoded items for testing
         auctionItems.put(1, new AuctionItem(1, "PC", "Windows", 100));
         auctionItems.put(2, new AuctionItem(2, "MAC", "Apple", 150));
    }

    public static void main(String[] args) {
        try {
            Server s = new Server();
            String name = "Auction";
            Auction stub = (Auction) UnicastRemoteObject.exportObject(s, 0);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(name, stub);
            System.out.println("Server ready");
        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }

    @Override
    public Integer register(String email) throws RemoteException {
        // Generate a unique userID for the user
        int userID = userIDs.size() + 1;
        userIDs.put(userID, userID);  // Storing the userID as the key and email as the value
        System.out.println("User registered: UserID=" + userID + ", Email=" + email);
        return userID;
    }
    

    @Override
    public AuctionItem getSpec(int itemID) throws RemoteException {
        return auctionItems.get(itemID);
    }

    @Override
    public Integer newAuction(int userID, AuctionSaleItem item) throws RemoteException {
        // Create a new auction with a unique auctionID
        int auctionID = auctionIDCounter++;
        AuctionItem auctionItem = new AuctionItem(auctionID, item.getName(), item.getDescription(), item.getStartingPrice());
        auctionItems.put(auctionID, auctionItem);
        System.out.println("New auction created: AuctionID=" + auctionID + ", Item=" + item.getName());
        return auctionID;
    }

    @Override
    public AuctionItem[] listItems() throws RemoteException {
        // Return an array of AuctionItem objects
        return auctionItems.values().toArray(new AuctionItem[0]);
    }

    @Override
    public AuctionResult closeAuction(int userID, int itemID) throws RemoteException {
        // Close an auction
        if (auctionItems.containsKey(itemID)) {
            AuctionItem item = auctionItems.get(itemID);
            int highestBid = item.getHighestBid();
            System.out.println("Auction closed: AuctionID=" + itemID + ", Highest Bid=" + highestBid);
            return new AuctionResult(itemID, highestBid);
        } else {
            System.out.println("Auction not found: AuctionID=" + itemID);
            return new AuctionResult(-1, -1);
        }
    }

    @Override
    public boolean bid(int userID, int itemID, int price) throws RemoteException {
        // Place a bid on an auction
        if (auctionItems.containsKey(itemID)) {
            AuctionItem item = auctionItems.get(itemID);
            int currentBid = item.getHighestBid();
            if (price > currentBid) {
                item.setHighestBid(price);
                System.out.println("Bid placed: UserID=" + userID + ", AuctionID=" + itemID + ", Bid=" + price);
                return true;
            } else {
                System.out.println("Bid rejected: UserID=" + userID + ", AuctionID=" + itemID + ", Bid=" + price);
                return false;
            }
        } else {
            System.out.println("Auction not found: AuctionID=" + itemID);
            return false;
        }
    }
}
