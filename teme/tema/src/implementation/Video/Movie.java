package implementation.Video;

import java.util.ArrayList;
import java.util.List;

public class Movie extends Video{
    private List<Double> ratings = new ArrayList<>();
    public Movie(String title, int year, ArrayList<String> genres, int duration) {
        super(title, year, genres);
        this.setDuration(duration);
    }

    public List<Double> getRatings() {
        return ratings;
    }

    public void setRatings(List<Double> ratings) {
        this.ratings = ratings;
    }
    public void addRating(double rating){
        this.ratings.add(rating);
    }
}
