package implementation.Actor;

import fileio.ActionInputData;
import fileio.ActorInputData;

import java.util.ArrayList;
import java.util.List;

public class Actor_dataBase {
    public ArrayList<Actor> actors = new ArrayList<>();
    public Actor_dataBase(List<ActorInputData> actors_list){
        for(ActorInputData actorInputData:actors_list){
            actors.add(new Actor(actorInputData.getName(), actorInputData.getCareerDescription(), actorInputData.getAwards(), actorInputData.getFilmography()));
        }
    }
}
