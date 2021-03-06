package com.android.myapplication.coldpod.network;

import com.android.myapplication.coldpod.network.data.LookupResult;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class LookupResponse {

    @SerializedName("results")
    private List<LookupResult> mLookupResults;

    public List<LookupResult> getLookupResults() {
        return mLookupResults;
    }

    public void setLookupResults(List<LookupResult> lookupResults) {
        mLookupResults = lookupResults;
    }
}
