package implementation.Video;

import java.util.ArrayList;

public class Series extends Video{
    private ArrayList<Seasons> seasons = new ArrayList<>();

    public Series(String title, int year, ArrayList<String> genres, ArrayList<Seasons> seasons) {
        super(title, year, genres);
        for(int i=0;i<seasons.size();i++){
            this.seasons.add(new Seasons(i+1, seasons.get(i).getDuration()));
        }
    }

    public ArrayList<Seasons> getSeasons() {
        return seasons;
    }

    public void setSeasons(ArrayList<Seasons> seasons) {
        this.seasons = seasons;
    }
}
