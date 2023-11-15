import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Auction extends Remote {
    public Integer register(String email) throws RemoteException;
    public AuctionItem getSpec(int itemID) throws RemoteException;
    public Integer newAuction(int userID, AuctionSaleItem item) throws RemoteException;
    public AuctionItem[] listItems() throws RemoteException;
    public AuctionResult closeAuction(int userID, int itemID) throws RemoteException;
    public boolean bid(int userID, int itemID, int price) throws RemoteException;
}
