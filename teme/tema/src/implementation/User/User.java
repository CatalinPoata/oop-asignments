package implementation.User;

import implementation.Video.Movie;
import implementation.Video.Movie_dataBase;
import implementation.Video.Series_dataBase;

import java.util.ArrayList;
import java.util.Map;

public class User {
    private String username;
    private String subscription;
    private final Map<String, Integer> history;
    private final ArrayList<String> favourites;
    private final ArrayList<String> rated = new ArrayList<>();
    private int actions;

    public User(String username, String subscription, Map<String, Integer> history, ArrayList<String> favourites){
        this.username=username;
        this.subscription=subscription;
        this.history=history;
        this.favourites = favourites;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public Map<String, Integer> getHistory() {
        return history;
    }

    public ArrayList<String> getFavourites() {
        return favourites;
    }

    public ArrayList<String> getRated() {
        return rated;
    }

    public int getActions() {
        return actions;
    }

    public void setActions(int actions) {
        this.actions = actions;
    }
    public String addFavorite(String v, Movie_dataBase mdb, Series_dataBase sdb);
    public String view(String v, Movie_dataBase mdb, Series_dataBase sdb);
    public String rate_movie(String v, double r, Movie_dataBase mdb);
    public String rate_series(String v, double r, Series_dataBase sdb);
}
