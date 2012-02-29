package it.unipr.aotlab;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import net.unto.twitter.TwitterProtos;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.LinkedList;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App 
{
    private final Twitter twitter;
    private AccessToken accessToken;

    static class TokenPair {
        final public long useId;
        final public AccessToken accessToken;

        public TokenPair(long useId, AccessToken accessToken) {
            this.useId = useId;
            this.accessToken = accessToken;
        }
    }

    public App() {
        twitter = new TwitterFactory().getInstance();
    }

    public Graph<String, Integer> mutualFriendships() {
        int counter = 0;
        List<TwitterProtos.User> friends = api.friends().build().get();
        List<String> friendNames = new LinkedList<String>();
        Graph<String, Integer> egoCenteredNetwork = new SparseGraph<String, Integer>();
        
        for(TwitterProtos.User friend : friends) {
            friendNames.add(friend.getName());
            egoCenteredNetwork.addVertex(friend.getName());
        }
        
        for(String lhs : friendNames) {
            for(String rhs : friendNames) {
                boolean friendshipExists = api.friendshipExists(lhs, rhs).build().get();
                if(friendshipExists) {
                    egoCenteredNetwork.addEdge(
                            counter++,
                            lhs,
                            rhs
                    );
                }
            }
        }

        return egoCenteredNetwork;
    }

    public void authenticate(String consumerKey, String consumerSecret, String filePath) throws TwitterException, IOException, ClassNotFoundException {
        twitter.setOAuthConsumer(consumerKey, consumerSecret);
        File authTokenFile = new File(filePath);
        if (authTokenFile.exists()) {
            TokenPair tokenPair = loadAccessToken(new FileInputStream(authTokenFile));
        } else {
            RequestToken requestToken = twitter.getOAuthRequestToken();
            accessToken = keyboardLogin(requestToken);
            storeAccessToken(twitter.verifyCredentials().getId() , accessToken);
        }
    }

    private AccessToken keyboardLogin(RequestToken requestToken) throws IOException {
        AccessToken accessToken = null;
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (null == accessToken) {
            System.out.println("Open the following URL and grant access to your account:");
            System.out.println(requestToken.getAuthorizationURL());
            System.out.print("Enter the PIN(if available) or just hit enter.[PIN]:");
            String pin = br.readLine();
            try{
                if(pin.length() > 0){
                    accessToken = twitter.getOAuthAccessToken(requestToken, pin);
                }else{
                    accessToken = twitter.getOAuthAccessToken();
                }
            } catch (TwitterException te) {
                if(401 == te.getStatusCode()){
                    System.out.println("Unable to get the access token.");
                }else{
                    te.printStackTrace();
                }
            }
        }
        return accessToken;
    }

    private static void storeAccessToken(long useId, AccessToken accessToken) throws IOException {
        OutputStream os = new FileOutputStream("access.dat");
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeLong(useId);
        oos.writeObject(accessToken);
    }

    private static TokenPair loadAccessToken(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        long useId = ois.readLong();
        AccessToken accessToken = (AccessToken)ois.readObject();
        return new TokenPair(useId, accessToken);
    }

    
    public <V, E> void drawGraph(Graph<V, E> graph) {
        Layout layout = new CircleLayout<V, E>(graph);
        layout.setSize(new Dimension(600,600));
        BasicVisualizationServer<Integer,String> vv =
                new BasicVisualizationServer<Integer,String>(layout);
        vv.setPreferredSize(new Dimension(650,650)); //Sets the viewing area size
        JFrame frame = new JFrame("Twitter Ego Centered Network");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(vv);
        frame.pack();
        frame.setVisible(true);

    }

    public static void main( String[] args )
    {
        App app = new App();


    }
}
