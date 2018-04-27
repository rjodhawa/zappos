package zappos.raunak.data.scientist;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Author: Raunak Jodhawat
 * Created on: 09/April/2018
 * contact: raunakjodhawat@gmail.com / rjodhawa@andrew.cmu.edu
 * Phone: 412-628-4310
 **/

//Servlet name and URL pattern 
@WebServlet(name = "raunakRestaurant", urlPatterns = {"/raunakRestaurant/*"})
public class serverservlet extends HttpServlet {

    // DB variables to be used to connect to mlab (cloud based MongoDB Platform)
    MongoClientURI uri;
    MongoClient client;
    MongoDatabase db;
    // Variable to store Server Replies to be displayed to the user after making the request
    public static String serverReply;
    public static String foodJoke;

    //Inititialize DB connection
    public void init() {
        uri = new MongoClientURI("<<mlab credentials>>");
        client = new MongoClient(uri);
        db = client.getDatabase(uri.getDatabase());
    }

    /* Handle get request
    Two types of request can be handeled by This servlet
    1. /raunakRestaurant/restaurant/{restaurant Name}/menu/{menu-type}/menuitem/{item}
        a. It Gets Price of a paticular {item} belonging to a paticular {menu-type} and a paticular {restaurant Name}
        b. Additionally, the page displays one random food joke from spoonacular.com/food-api
        c. Also, Wine is recommended on the basis of {item}. Wine recommendation is taken from spoonacular.com/food-api
        d. Lastly, Image of the {item} is diplayed by using flickr API
    2. /raunakRestaurant/offers
        a. Gets all offers currently available with the system.
        b. Prints a random food joke, too.
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Get in the request parameter
        String queryParameter = request.getPathInfo().substring(1);

        // if query is of type /raunakRestaurant/restaurant/{restaurant Name}/menu/{menu-type}/menuitem/{item}
        if (queryParameter.contains("restaurant/") && queryParameter.contains("menu/") && queryParameter.contains("menuitem/")) {
            // get the restaurant Name, menu (lunch, breakfast, dinner, bar), menuitem
            String restaurantName = queryParameter.substring(queryParameter.indexOf("restaurant/") + 11, queryParameter.indexOf("menu/") - 1);
            String menu = queryParameter.substring(queryParameter.indexOf("menu/") + 5, queryParameter.indexOf("menuitem/") - 1);
            String menuitem = queryParameter.substring(queryParameter.indexOf("menuitem/") + 9, queryParameter.length());
            // mongo Collection
            MongoCollection<Document> collection = db.getCollection("restaurants");
            // Perfrom the search query
            MongoCursor cursor = collection.find(new Document("menu." + menu + "." + menuitem, new Document("$gt", "0.00$"))
                    .append("name", restaurantName)).iterator();
            // If query hit was successful
            if (cursor.hasNext()) {
                try {
                    while (cursor.hasNext()) {
                        Document doc = (Document) cursor.next();
                        // Get wine recommendation for {item} spoonacular food-api
                        getWineRecommendation(menuitem, request);
                        // Get picture of {item} from flickr api
                        getFlickrURL(menuitem, request);
                        // Set attribute: server reply will be displayed on webView.jsp
                        request.setAttribute("serverReply", doc.get("name") + " Servers " + menuitem + " at the price of " + ((Document) ((Document) doc.get("menu")).get(menu)).get(menuitem) + " in there " + menu + " Menu");
                    }
                } finally {
                    cursor.close();
                }
            } else {
                // Else, our DB does not contain those value
                response.setStatus(400, "Please check query paramaeters");
            }
        } else if (queryParameter.contains("offers")) {
            // /raunakRestaurant/offers
            MongoCollection<Document> collection = db.getCollection("offers");
            // get all the documents in offers
            collection.find().forEach(printBlock);
            // set the global variable with the value calculated in printBlock
            request.setAttribute("serverReply", serverservlet.serverReply);
        } else {
            // /raunakRestaurant/{anything else is illegal} - set the response code to 400
            response.setStatus(400, "illegal URL");
        }
        // Get a random food joke from spoonacular food -api
        String foodJoke = randomFoodJoke();
        request.setAttribute("foodJoke", foodJoke);
        // Change the view to JSP page and display the results
        RequestDispatcher view = request.getRequestDispatcher("/webView.jsp");
        view.forward(request, response);
    }

    /* Handles adding offer to the the database
    POST http://localhost:8080/Raunak-Zappos-Restaurant/raunakRestaurant/ 
    Headers : 
            rest = {restname},
            Main = {main-heading-of-the-offer},
            Small = {small-heading},
            id = {unique-id-deletion}
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Get Header values from the post request
        String restName = request.getHeader("rest");
        String mainHeading = request.getHeader("Main");
        String smallHeading = request.getHeader("Small");
        String id = request.getHeader("id");
        // If anything is missing, report error
        if (restName == null || mainHeading == null || smallHeading == null || id == null) {
            response.setStatus(400, "Please check header and provide complete list of headers");
        } else {
            // Add new offer to the database
            Document document = new Document("rest", restName)
                    .append("Main", mainHeading)
                    .append("Small", smallHeading)
                    .append("id", id);
            MongoCollection<Document> collection = db.getCollection("offers");
            // Add the offer to mlab
            collection.insertOne(document);
            // set the attribute serverReply to display appropriate message
            request.setAttribute("serverReply", "Your offer was added: <br>Restaurant Name: " + restName + "<br>Main Heading: " + mainHeading + "<br>Small Heading: " + smallHeading + "<br>id: " + id);
        }
        // Change the view to JSP page and display the results
        RequestDispatcher view = request.getRequestDispatcher("/webView.jsp");
        view.forward(request, response);
    }

    /* Handles deleting offer from the the database, searched by the id
    DELETE http://localhost:8080/Raunak-Zappos-Restaurant/raunakRestaurant/ 
    Headers : 
            id = {offer-id-to-delete},
    Response:
        Header field: "serverReply"
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = request.getHeader("id");
        MongoCollection<Document> collection = db.getCollection("offers");
        if (collection.deleteOne(eq("id", id)).toString().contains("deletedCount=1")) {
            response.setHeader("serverReply", "Offer with the id: " + id + " was deleted");
        } else {
            response.setStatus(400, "Could not find the offer with the given id");
        }
    }

    /* Handles changing price for a paticular menu-item belonging to a paticular restaurant and menu
    PUT http://localhost:8080/Raunak-Zappos-Restaurant/raunakRestaurant/ 
    Headers : 
            rest = {restname},
            menu = {menu-type},
            menuitem = {item},
            newprice = {newer-price}
    Response:
        Header field: "serverReply"
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // get restaurant name, menu, menu item from the headers
        String restaurantName = request.getHeader("rest");
        String menu = request.getHeader("menu");
        String menuitem = request.getHeader("menuitem");
        String newPrice = request.getHeader("newprice");
        MongoCollection<Document> collection = db.getCollection("restaurants");
        // check if prices are updated
        if (collection.updateOne(eq("name", restaurantName), set("menu." + menu + "." + menuitem, newPrice)).toString().contains("modifiedCount=1")) {
            response.setHeader("serverReply", "Restaurant Name: " + restaurantName + ", Menu: " + menu + ", Menu Item: " + menuitem + ", Updated with Price of: " + newPrice);
        } else {
            response.setStatus(400, "Failed to find the resource");
        }
    }
    // To print all the offers available in the DB
    Block<Document> printBlock = new Block<Document>() {
        String localReplyCreation = "";
        @Override
        public void apply(final Document document) {
            try {
                // Convert Document to JSON and gets its element
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(document.toJson());
                localReplyCreation += "<h3>OFFER at! :" + json.get("rest") + " </h3>" + json.get("Main") + " ---- Info: " + json.get("Small") + "<br>";
            } catch (ParseException ex) {
                Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
            }
            // Set the global variable, to be displayed on webView.jsp
            serverservlet.serverReply = localReplyCreation;
        }
    };

    // This function deals with getting a random food joke from spoonacular food-api
    protected String randomFoodJoke() {
        URL wineURL;
        try {
            // URL
            wineURL = new URL("https://spoonacular-recipe-food-nutrition-v1.p.mashape.com/food/jokes/random");
            HttpURLConnection con = (HttpURLConnection) wineURL.openConnection();
            // Set headers
            con.setRequestProperty("X-Mashape-Key", "<<X-Mashape-Key>>");
            con.setRequestProperty("X-Mashape-Host", "spoonacular-recipe-food-nutrition-v1.p.mashape.com");
            con.setRequestMethod("GET");
            // Connect
            con.connect();
            // Get the input and return the text of the joke returned from spoonacular
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String str = in.readLine();
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(str);
            return json.get("text").toString();
        } catch (MalformedURLException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException | ParseException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    // This funciton deals with getting wine recommendation for a paticular menu item
    protected void getWineRecommendation(String menuitem, HttpServletRequest request) {
        URL wineURL;
        try {
            // URL
            wineURL = new URL("https://spoonacular-recipe-food-nutrition-v1.p.mashape.com/food/wine/pairing?maxPrice=50&food=" + menuitem);
            HttpURLConnection con = (HttpURLConnection) wineURL.openConnection();
            // Header
            con.setRequestProperty("X-Mashape-Key", "<<X-Mashape-Key>>");
            con.setRequestProperty("X-Mashape-Host", "spoonacular-recipe-food-nutrition-v1.p.mashape.com");
            con.setRequestMethod("GET");
            // Connect
            con.connect();
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String str = in.readLine();
            JSONParser parser = new JSONParser();
            JSONObject json = (JSONObject) parser.parse(str);
            // Parse JSON to get the text of wine pairing returned from spoonacular api
            request.setAttribute("wineRecommendation", json.get("pairingText").toString());
        } catch (MalformedURLException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ProtocolException | ParseException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    //This funciton deals with getting image of a menu item by using flickr API
    protected void getFlickrURL(String menuitem, HttpServletRequest request) {
        try {
            String pictureURL = null;
            //URL
            URL flickrURL = new URL("https://api.flickr.com/services/rest/?method=flickr.photos.search&api_key=<<flickr_key>>&is_getty=true&tags=" + menuitem);
            HttpURLConnection conn = (HttpURLConnection) flickrURL.openConnection();
            // Connect
            conn.connect();
            BufferedReader ind = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String strd;
            String result = null;
            // Read result obtained from flickr
            // Read each line of "in" until done, adding each to "response"
            while ((strd = ind.readLine()) != null) {
                result += strd;
            }
            // Parse to get the URL paramaeters
            String farm = result.substring(result.indexOf("farm=") + 6, result.indexOf("\"", result.indexOf("farm=") + 6));
            String server = result.substring(result.indexOf("server=") + 8, result.indexOf("\"", result.indexOf("server=") + 8));
            String id = result.substring(result.indexOf("id=") + 4, result.indexOf("\"", result.indexOf("id=") + 4));
            String secret = result.substring(result.indexOf("secret=") + 8, result.indexOf("\"", result.indexOf("secret=") + 8));
            // Make the picture URL
            pictureURL = "<img src= \"http://farm" + farm + ".static.flickr.com/" + server + "/" + id + "_" + secret + "_z.jpg\" width=\"250\" height=\"345\">";
            // Set the pictureURL attribute
            request.setAttribute("pictureURL", pictureURL);
        } catch (IOException ex) {
            Logger.getLogger(serverservlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
