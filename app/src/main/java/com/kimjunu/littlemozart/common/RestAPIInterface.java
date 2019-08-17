package com.kimjunu.littlemozart.common;

import com.kimjunu.littlemozart.App;
import com.kimjunu.littlemozart.model.LittleMozartData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface RestAPIInterface {
    @POST(App.URL_LITTLE_MOZART)
    Call<LittleMozartData> callLittleMozart(@Body LittleMozartData body);
}
