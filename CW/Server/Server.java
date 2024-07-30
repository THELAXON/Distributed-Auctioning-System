import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Server implements Auction {
    private Map<Integer, AuctionItem> auctionItems = new HashMap<>(); // stores the auction items.
    private Map<Integer, String> userEmails = new HashMap<>(); // stores the userID and email.
    private Map<Integer, PublicKey> userKeyPairs = new HashMap<>(); // Stores the user ID along with their public keys when they communicate with the server.
    private Map<Integer, TokenInfo> activeTokens = new HashMap<>(); // Stores the active tokens that exist along with the user ID that owns it.
    private Map<Integer, String> challengeMap = new HashMap<>(); // Stores the challenges of the client along with their ID.
    private Map<Integer, Integer> highestBidder = new HashMap<>();  // Keeps track of the highest bidder based on an item so that when we close a bid the correct bidder is declared the winner.
    private Map<Integer, Integer> auctionCreators = new HashMap<>(); // keeps track of the auctions that are created and the user who creates it so nobody else can take down an auction if it isn't theirs.
    private int itemIDCounter = 1; // used to create itemID

    private PrivateKey serverPrivateKey; // Variable used to store server's private key
    private PublicKey serverPublicKey; // Variable used to store server's public key

    public Server() throws RemoteException {
        super();
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA"); // Generate keys for server when communicating based on RSA encryption.
            keyPairGenerator.initialize(2048);
            KeyPair keyPair=keyPairGenerator.generateKeyPair();
            serverPrivateKey = keyPair.getPrivate();
            serverPublicKey = keyPair.getPublic();
            storePublicKey(serverPublicKey, "../keys/serverKey.pub");
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    public Integer register(String email, PublicKey pubKey) throws RemoteException {  // Allows the user to register if they have an email that hasn't been used before along with their public key.
    if (userEmails.containsValue(email)) {
        System.out.println("The email has an account");
        return null;
    } else {
        // Generate a unique userID for the user
        int userID = userEmails.size() + 1;
        userEmails.put(userID, email);  // Storing the email associated with the userID
        userKeyPairs.put(userID, pubKey);
        System.out.println("User registered: UserID=" + userID + ", Email=" + email);
        return userID;
    }
}

    // Method to write a public key to a file.
    // Example use: storePublicKey(aPublicKey, ‘../keys/serverKey.pub’)
    public void storePublicKey(PublicKey publicKey, String filePath) throws Exception {
    // Convert the public key to a byte array
    byte[] publicKeyBytes = publicKey.getEncoded();
    // Encode the public key bytes as Base64
    String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKeyBytes);
    // Write the Base64 encoded public key to a file
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
        fos.write(publicKeyBase64.getBytes());
        }
    }

    @Override
    public ChallengeInfo challenge(int userID, String clientChallenge) throws RemoteException {   // A challenge is randomly generted to be given to the server from the client using this method
        try {
            System.out.println("Challenge requested for UserID=" + userID); // Debug

            // Generate a random challenge for the client
            SecureRandom random = new SecureRandom();
            byte[] serverChallengeBytes = new byte[8];
            random.nextBytes(serverChallengeBytes);
            String serverChallenge = Base64.getEncoder().encodeToString(serverChallengeBytes);

            // Sign the client's challenge using the server's private key
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(serverPrivateKey);
            sign.update(clientChallenge.getBytes());
            byte[] serverResponse = sign.sign();

            // Store the client challenge for later authentication
            challengeMap.put(userID, clientChallenge);

            // Return ChallengeInfo with server's response and server's challenge to the client
            ChallengeInfo challengeInfo = new ChallengeInfo();
            challengeInfo.response = serverResponse;
            challengeInfo.serverChallenge = serverChallenge;

            // Before sending the challenge to the client
            System.out.println("Sending ChallengeInfo: " + challengeInfo);

            return challengeInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public TokenInfo authenticate(int userID, byte[] signature) throws RemoteException {     //Authenticates the client when client calls it and lets the user get a valid token
        try {
            System.out.println("Authenticating user: UserID=" + userID); // Debug

            String serverChallenge = challengeMap.get(userID);
            PublicKey clientPublicKey = userKeyPairs.get(userID);
        
            // Verify the client's signature using the server's public key
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(clientPublicKey);
            sig.update(serverChallenge.getBytes());
    
            if (sig.verify(signature)) {
                // Generate a one-time use token and set its expiration time
                String token = generateToken();
                long expiryTime = System.currentTimeMillis() + 10000;  // Token expires in 10 seconds
                //activeTokens.put(token, expiryTime);
    
                // Return TokenInfo with the generated token and its expiration time
                TokenInfo tokenInfo = new TokenInfo();
                tokenInfo.token = token;
                activeTokens.put(userID, tokenInfo);
                // After authentication
                System.out.println("Authentication successful. Sending TokenInfo: " + tokenInfo);
    
                tokenInfo.expiryTime = expiryTime;
    
                return tokenInfo;
    
            } else {
                System.out.println("Signature verification failed for UserID=" + userID); // Debug
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public AuctionItem getSpec(int userID, int itemID, String token) throws RemoteException { // returns the item information if the token is valid and the itemID is valid
        if (validateToken(userID, token)) {
            return auctionItems.get(itemID);
        }
        return null;
    }

    @Override
    public Integer newAuction(int userID, AuctionSaleItem item, String token) throws RemoteException {  // Creates a new auction by checking if the user has a valid token and takes the information of the item.
        if (validateToken(userID, token)) {
            if (userEmails.get(userID) != null) {
                int itemID = itemIDCounter++;
                AuctionItem auctionItem = new AuctionItem();
                auctionItem.itemID = itemID;
                auctionItem.name = item.name;
                auctionItem.description = item.description;
                auctionItem.highestBid = item.reservePrice;
                auctionItems.put(itemID, auctionItem);
                auctionCreators.put(itemID, userID);  // Store the creator's userID
                System.out.println("New auction created: itemID=" + itemID + ", Item=" + item.name);
                return itemID;
            } else {
                System.out.println("You need to have an account to bid");
                return null;
            }
        }
        return null;
    }


    @Override
    public AuctionItem[] listItems(int userID, String token) throws RemoteException {       // Lists all current items if the token is valid for the user requesting it otherwise it will say the user has an invalid token.
        if (validateToken(userID, token)) {
            return auctionItems.values().toArray(new AuctionItem[0]);
        } else {
            System.out.println("Invalid token for listItems: UserID =" + userID);
            return new AuctionItem[0]; // Return an empty array if the token is invalid
        }
    }

    @Override
    public AuctionResult closeAuction(int userID, int itemID, String token) throws RemoteException {     // Checks if the token is a valid token and then checks if the item exists, if it does it checks if the person closing it is the auction creator and if they are it allows them to declare the winning bidder by using the highestbidder hashmap
        if (validateToken(userID, token)) {
            // Close an auction
            if (auctionItems.containsKey(itemID)) {
                AuctionItem item = auctionItems.get(itemID);
    
                // Verify that the user closing the auction is the creator
                int creatorUserID = auctionCreators.get(itemID);
                if (creatorUserID == userID) {
                    // Find the highest bidder for this item
                    int highestBidderUserID = highestBidder.get(itemID);
    
                    // Retrieve the user's email using the userID of the highest bidder
                    String winningEmail = userEmails.get(highestBidderUserID);
                    
                    int highestBid = item.highestBid;
    
                    System.out.println("Auction closed: AuctionID=" + itemID + ", Highest Bid=" + highestBid +
                            ", Winning Email=" + winningEmail);
    
                    // Create and return the AuctionResult
                    AuctionResult auctionresult = new AuctionResult();
                    auctionresult.winningEmail = winningEmail;
                    auctionresult.winningPrice = highestBid;
                    return auctionresult;
                } else {
                    System.out.println("Unauthorized user tried to close the auction: UserID=" + userID +
                            ", AuctionID=" + itemID);
                    return null;
                }
            } else {
                System.out.println("Auction not found: AuctionID=" + itemID);
                return null;
            }
        }
        return null;
    }
    
    @Override
    public boolean bid(int userID, int itemID, int price, String token) throws RemoteException { // allows a user who has a valid token to bid and if the bid is higher than the current bid on an item then it gets replaced and the bidder ID is stored in a hashmap
        if (validateToken(userID, token)) {
            // Access control check: Only allow bids on existing auctions
            if (auctionItems.containsKey(itemID)) {
                AuctionItem item = auctionItems.get(itemID);
    
                // Check if the bid is higher than the current highest bid
                if (price > item.highestBid) {
                    // Update the highest bidder for this item
                    highestBidder.put(itemID, userID);
    
                    // Update the highest bid for this item
                    item.highestBid = price;
    
                    System.out.println("Bid placed: UserID=" + userID + ", AuctionID=" + itemID + ", Bid=" + price);
                    return true;
                } else {
                    System.out.println("Bid rejected: UserID=" + userID + ", AuctionID=" + itemID + ", Bid=" + price +
                            " (Not higher than current highest bid so please place a higher bid)");
                    return false;
                }
            } else {
                System.out.println("Auction not found: AuctionID=" + itemID);
                return false;
            }
        }
        return false;
    }
    


    private String generateToken() {                                 // Used to generate a random string token that is 8 bytes.
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[8];
        random.nextBytes(tokenBytes);
        return Base64.getEncoder().encodeToString(tokenBytes);
    }

    private boolean validateToken(int userID, String token) {        //Deletes the token after checking if the token is an active token and as all tokens are one time use they are deleted afterwards and return true.
        TokenInfo activeToken = activeTokens.get(userID);
        if (activeToken != null && activeToken.token.equals(token) && System.currentTimeMillis() < activeToken.expiryTime) {
            activeTokens.remove(userID);
            return true;
        } else {
            return false;
        }
    }
}