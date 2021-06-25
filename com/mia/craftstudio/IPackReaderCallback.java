package com.mia.craftstudio;


import com.google.gson.JsonElement;

public interface IPackReaderCallback {
    void modelLoaded(CSModel model, JsonElement json);
    void setCount(int count);

}
