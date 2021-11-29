package implementation.Actor;

import actor.ActorsAwards;

import java.util.ArrayList;
import java.util.Map;

public class Actor {
    private double average;
    private String name;
    private String career_description;
    private Map<ActorsAwards, Integer> actor_awards;
    private ArrayList<String> videos = new ArrayList<>();

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCareer_description() {
        return career_description;
    }

    public void setCareer_description(String career_description) {
        this.career_description = career_description;
    }

    public Map<ActorsAwards, Integer> getActor_awards() {
        return actor_awards;
    }

    public void setActor_awards(Map<ActorsAwards, Integer> actor_awards) {
        this.actor_awards = actor_awards;
    }

    public ArrayList<String> getVideos() {
        return videos;
    }

    public void setVideos(ArrayList<String> videos) {
        this.videos = videos;
    }

    public Actor(String name, String career_description, Map<ActorsAwards, Integer> actor_awards, ArrayList<String> videos){
        this.name=name;
        this.career_description=career_description;
        this.actor_awards=actor_awards;
        this.videos=videos;
    }
}
