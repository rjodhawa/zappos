# Use case
I have built a community-restaurant-based API in which user posts or deletes the offer. It’s like you walk down the road you see the offer outside a restaurant and you post it via a POST request.
Now any other user (Probably someone who is hungry and smart enough to save money) will make the GET request to get all the offers.
He visits the restaurant, only to find that the offer was removed. This user will then send a DELETE request with the offer id and the offer would be deleted from our Database.
You saw the offer, but wouldn’t it be nice to see if a restaurant serves a dish for their lunch/ dinner/ breakfast or bar menu. You send a GET request and you are greeted with a price of the dish for your selection and wine recommendation.
To keep app interesting, after every request user makes, a random food joke is delivered to him.
--------------------------------------------------
Get started with API: (Just Click the link)
http://zappos-raunak.herokuapp.com/raunakRestaurant/offers
http://zappos-raunak.herokuapp.com/raunakRestaurant/restaurant/all%20day%20eats/menu/lunch/menuitem/steak

API End points can be accessed via:
GET http://zappos-raunak.herokuapp.com/raunakRestaurant/restaurant/{restaurant-Name}/menu/{menu-type}/menuitem/{item}
{restaurant-Name}   	    {menu-type}               {item}
All Day Eats		    Lunch		      Steak
				                      Salmon
All Day Eats		    Dinner		      Corned Beef
				                      Omelet
All Day Eats		    Bar		              Roast Beef
Make out meals		    Lunch		      Turkey Sausage
				                      Salmon
Night Hungers		    Dinner		      Tuna Salad
				                      Crispy Jumbo Fish	
Happy Mornings              Breakfast	    	      rotisserie chicken breast
				                      bbq-chicken
Happy Mornings      	    Lunch		      Turkey Sausage

# Caution
You would want to add in your personal database configurations and API key from spoonacular, flikcr in following places:
zappos/src/java/zappos/raunak/data/scientist/serverservlet.java
uri = new MongoClientURI("<<mlab credentials>>");
randomFoodJoke() & getWineRecommendation()
con.setRequestProperty("X-Mashape-Key", "<<X-Mashape-Key>>");
flickrURL = new URL(<<flickr_key>>);
