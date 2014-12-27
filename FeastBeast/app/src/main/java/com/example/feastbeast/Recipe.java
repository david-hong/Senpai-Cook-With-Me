package com.example.feastbeast;

import java.util.List;
import java.util.ArrayList;

/**
 * Created by davidhong12 on 2014-12-21.
 */
public class Recipe {
    String name;
    List<String> ingredints;
    List<String> recipe;

    public Recipe(String n, List<String> ingred, List<String> recip){
        this.name = n;
        this.ingredints = ingred;
        this.recipe = recip;
    }

    public String toString(){
        return this.name;
    }
}
