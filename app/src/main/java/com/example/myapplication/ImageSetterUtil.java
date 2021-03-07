package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class ImageSetterUtil {

    public Map<String,Integer > getModelsMap(){
        Map<String,Integer > map = new HashMap<>();
        map.put("deer",R.raw.deer);
        map.put("camera",R.raw.camera);
        map.put("butterfly",R.raw.butterfly);
        map.put("plant",R.raw.plant);
        return map;
    }
}
