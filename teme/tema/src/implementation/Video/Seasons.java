package implementation.Video;

import java.util.ArrayList;
import java.util.List;

public class Seasons {
    private final int currentSeason;
    private int duration;
    private List<Double> ratings = new ArrayList<>();

    public int getCurrentSeason() {
        return currentSeason;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public List<Double> getRatings() {
        return ratings;
    }

    public void setRatings(List<Double> ratings) {
        this.ratings = ratings;
    }
    public Seasons(final int currentSeason, final int duration){
        this.currentSeason=currentSeason;
        this.duration=duration;
    }
    public void addRating(double rating){
        this.ratings.add(rating);
    }

    @Override
    public String toString() {
        return "Episode{" +
                "currentSeason=" + currentSeason +
                ", duration=" + duration +
                '}';
    }
}
