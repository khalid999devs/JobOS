package com.jobos.android.data.network;

public interface ApiCallback<T> {
    void onSuccess(T response);
    void onError(String error);
}
