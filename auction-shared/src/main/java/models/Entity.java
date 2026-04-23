package models;

import java.util.*;

public abstract class Entity {
    protected String id;

    public Entity(){
        this.id = UUID.randomUUID().toString();
    }

    public String getId(){
        return id;
    }

    public void setId(String id){
        this.id = id;
    }
}
