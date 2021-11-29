package implementation.User;

import fileio.UserInputData;
import implementation.Video.Movie_dataBase;
import implementation.Video.Series_dataBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User_dataBase {
    Map<String, User> users=new HashMap<>();
    public User_dataBase(List<UserInputData> users) {
        for(UserInputData user:users){
            this.users.put(user.getUsername(), new User(user.getUsername(), user.getSubscriptionType(), user.getHistory(), user.getFavoriteMovies()));
        }
    }
    public String addFavourite(String name, String title, Movie_dataBase mdb, Series_dataBase sdb);
    public String addMovieRating(String name, String title, Movie_dataBase mdb, double rating);
    public String addSeriesRating(String name, String title, Series_dataBase sdb, double rating, int season_no);
    public String addViews(String name, String title, Movie_dataBase mdb, Series_dataBase sdb);
}
