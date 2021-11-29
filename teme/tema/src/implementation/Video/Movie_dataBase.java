package implementation.Video;

import fileio.MovieInputData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Movie_dataBase {
    Map<String, Movie> movies = new HashMap<>();
    public Movie_dataBase(List<MovieInputData> movies){
        for(MovieInputData movie:movies){
            this.movies.put(movie.getTitle(), new Movie(movie.getTitle(), movie.getYear(), movie.getGenres(), movie.getDuration()));
        }
    }
    public void addRating(double rating, String title){
        this.movies.get(title).addRating(rating);
    }
    public boolean existsMovie(String title){
        return this.movies.containsKey(title);
    }
    public void addViews(String title){
        this.movies.get(title).addViews();
    }
    public void addFavourite(String title){
        this.movies.get(title).addFavourites();
    }
}
