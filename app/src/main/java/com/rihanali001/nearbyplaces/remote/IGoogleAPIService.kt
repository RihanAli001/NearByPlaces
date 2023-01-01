package com.rihanali001.nearbyplaces.remote

import com.rihanali001.nearbyplaces.model.MyPlaces
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Url

interface IGoogleAPIService {
    @GET
    fun getNearbyPlaces(@Url url: String): Call<MyPlaces>
}