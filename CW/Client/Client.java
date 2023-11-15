//import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {
    public static void main(String[] args) {
        try {
            String name = "Auction";
            Registry registry = LocateRegistry.getRegistry("localhost");
            Auction server = (Auction) registry.lookup(name);

            // User registration
            int userID = server.register("user@example.com");

            // Create a new auction
            AuctionSaleItem newItem = new AuctionSaleItem("Laptop", "Brand new laptop", 200);
            int auctionID = server.newAuction(userID, newItem);

            // List open auctions
            AuctionItem[] items = server.listItems();
            System.out.println("Open Auctions:");
            for (AuctionItem item : items) {
                System.out.println("AuctionID: " + item.getItemID() +
                        ", Name: " + item.getName() +
                        ", Description: " + item.getDescription() +
                        ", Highest Bid: " + item.getHighestBid());
            }

            // Place a bid
            boolean bidSuccess = server.bid(userID, auctionID, 250);
            if (bidSuccess) {
                System.out.println("Bid placed successfully on AuctionID " + auctionID);
            } else {
                System.out.println("Bid rejected on AuctionID " + auctionID);
            }

            // Close an auction
            AuctionResult result = server.closeAuction(userID, auctionID);
            System.out.println("Auction closed: AuctionID=" + result.getAuctionID() +
                    ", Highest Bid=" + result.getHighestBid());

        } catch (Exception e) {
            System.err.println("Exception:");
            e.printStackTrace();
        }
    }
}



// //import java.rmi.RemoteException;
// import java.rmi.registry.LocateRegistry;
// import java.rmi.registry.Registry;

// public class Client {
//     public static void main(String[] args) {
//         try {
//             String name = "Auction";
//             Registry registry = LocateRegistry.getRegistry("localhost");
//             Auction server = (Auction) registry.lookup(name);

//             // User registration
//             int userID = server.register("user@example.com");

//             // Create a new auction
//             AuctionSaleItem newItem = new AuctionSaleItem("Laptop", "Brand new laptop", 200);
//             int auctionID = server.newAuction(userID, newItem);




//             // // List open auctions
//             // AuctionItem[] items = server.listItems();
//             // System.out.println("Open Auctions:");
//             // for (AuctionItem item : items) {
//             //     System.out.println(item);
//             // }

//             // // Place a bid
//             // boolean bidSuccess = server.bid(userID, auctionID, 250);
//             // if (bidSuccess) {
//             //     System.out.println("Bid placed successfully.");
//             // } else {
//             //     System.out.println("Bid rejected.");
//             // }

//             // // Close an auction
//             // AuctionResult result = server.closeAuction(userID, auctionID);
//             // System.out.println("Auction closed: AuctionID=" + result.getAuctionID() +
//             //                    ", Highest Bid=" + result.getHighestBid());

//         } catch (Exception e) {
//             System.err.println("Exception:");
//             e.printStackTrace();
//         }
//     }
// }
