package com.example.myapplication;

import java.util.HashMap;
import java.util.Map;

public class ImageSetterUtil {

    public Map<String,Integer > getModelsMap(){
        Map<String,Integer > map = new HashMap<>();
        map.put("clock",R.raw.clock);
        map.put("camera",R.raw.camera);
        map.put("ukulele",R.raw.ukulele);
        map.put("plant",R.raw.plant);
        return map;
    }
}
