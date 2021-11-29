package implementation.Video;

import java.util.ArrayList;

public class Video {
    private String title;
    private int year;
    private int duration;
    private ArrayList<String> genres;
    private int favourites = 0;
    private int views = 0;
    private double final_rating;

    public Video(String title, int year, ArrayList<String> genres){
        this.title=title;
        this.year=year;
        this.genres=genres;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public ArrayList<String> getGenres() {
        return genres;
    }

    public void setGenres(ArrayList<String> genres) {
        this.genres = genres;
    }

    public int getFavourites() {
        return favourites;
    }

    public void setFavourites(int favourites) {
        this.favourites = favourites;
    }

    public int getViews() {
        return views;
    }

    public void setViews(int views) {
        this.views = views;
    }

    public void addFavourites(){
        this.favourites++;
    }
    public void addViews(){
        this.views++;
    }

    public double getFinal_rating() {
        return final_rating;
    }

    public void setFinal_rating(double final_rating) {
        this.final_rating = final_rating;
    }
}
