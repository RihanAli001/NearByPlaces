package com.rihanali001.nearbyplaces

import com.rihanali001.nearbyplaces.model.Results
import com.rihanali001.nearbyplaces.remote.IGoogleAPIService
import com.rihanali001.nearbyplaces.remote.RetrofitClient

object Common {
    var currentResult: Results? = null

    private val GOOGLE_API_URL = "https://maps.googleapis.com/"
    val googleApiService: IGoogleAPIService
        get() = RetrofitClient.getClient(GOOGLE_API_URL).create(IGoogleAPIService::class.java)
}