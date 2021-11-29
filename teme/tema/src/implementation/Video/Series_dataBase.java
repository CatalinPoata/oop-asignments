package implementation.Video;

import fileio.SerialInputData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Series_dataBase {
    Map<String, Series> series = new HashMap<>();
    public Series_dataBase(List<SerialInputData> series){
        for(SerialInputData serial:series){
            this.series.put(serial.getTitle(), new Series(serial.getTitle(), serial.getYear(), serial.getGenres(), serial.getSeasons()));
        }
    }
    public void addRating(double rating, String title, int season_no){
        series.get(title).getSeasons().get(season_no-1).addRating(rating);
    }
    public boolean isRated(String title, int season_no){
        return series.get(title).getSeasons().get(season_no-1).getRatings().isEmpty();
    }
    public void addViews(String title){
        series.get(title).addViews();
    }
    public void addFavourite(String title){
        series.get(title).addFavourites();
    }
}
