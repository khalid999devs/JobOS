package com.jobos.android.data.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.android.config.ApiConfig;
import com.jobos.android.data.local.SessionManager;
import com.jobos.android.data.model.auth.AuthResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.OkHttpClient;

/**
 * Interceptor that automatically handles 401 Unauthorized errors by refreshing the access token.
 * If refresh fails, triggers logout.
 */
public class AuthInterceptor implements Interceptor {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final Object refreshLock = new Object();
    private volatile boolean isRefreshing = false;
    private AuthEventListener authEventListener;

    public interface AuthEventListener {
        void onLogoutRequired();
    }

    public AuthInterceptor(Context context) {
        this.sessionManager = new SessionManager(context);
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setAuthEventListener(AuthEventListener listener) {
        this.authEventListener = listener;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Response response = chain.proceed(originalRequest);

        // Check if we got a 401 Unauthorized
        if (response.code() == 401) {
            // Don't try to refresh for auth endpoints
            String url = originalRequest.url().toString();
            if (url.contains("/api/auth/")) {
                return response;
            }

            // Check if we have a refresh token
            String refreshToken = sessionManager.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                triggerLogout();
                return response;
            }

            synchronized (refreshLock) {
                // Check again in case another thread already refreshed
                String currentToken = sessionManager.getAccessToken();
                String originalAuthHeader = originalRequest.header("Authorization");
                
                if (originalAuthHeader != null && currentToken != null) {
                    // Token was already refreshed by another thread
                    if (!originalAuthHeader.equals("Bearer " + currentToken)) {
                        response.close();
                        Request newRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + currentToken)
                                .build();
                        return chain.proceed(newRequest);
                    }
                }

                if (!isRefreshing) {
                    isRefreshing = true;
                    try {
                        // Close the previous response
                        response.close();

                        // Try to refresh the token synchronously
                        AuthResponse authResponse = refreshTokenSync(refreshToken);
                        
                        if (authResponse != null && authResponse.getAccessToken() != null) {
                            // Save new tokens
                            sessionManager.saveAuthTokens(
                                    authResponse.getAccessToken(),
                                    authResponse.getRefreshToken()
                            );

                            // Retry the original request with new token
                            Request newRequest = originalRequest.newBuilder()
                                    .header("Authorization", "Bearer " + authResponse.getAccessToken())
                                    .build();
                            return chain.proceed(newRequest);
                        } else {
                            triggerLogout();
                        }
                    } finally {
                        isRefreshing = false;
                    }
                }
            }
        }

        return response;
    }

    private AuthResponse refreshTokenSync(String refreshToken) {
        try {
            OkHttpClient tempClient = new OkHttpClient.Builder().build();
            
            Map<String, String> body = new HashMap<>();
            body.put("refreshToken", refreshToken);
            String json = objectMapper.writeValueAsString(body);

            Request refreshRequest = new Request.Builder()
                    .url(ApiConfig.getBaseUrl() + "/api/auth/refresh")
                    .post(RequestBody.create(json, JSON))
                    .build();

            Response refreshResponse = tempClient.newCall(refreshRequest).execute();
            
            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                String responseBody = refreshResponse.body().string();
                // Parse the response - backend might return directly or in a result wrapper
                try {
                    // Try parsing as direct AuthResponse first
                    return objectMapper.readValue(responseBody, AuthResponse.class);
                } catch (Exception e) {
                    // Try parsing as wrapped response
                    Map<String, Object> wrapper = objectMapper.readValue(responseBody, 
                            new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                    Object result = wrapper.get("result");
                    if (result != null) {
                        String resultJson = objectMapper.writeValueAsString(result);
                        return objectMapper.readValue(resultJson, AuthResponse.class);
                    }
                }
            }
            refreshResponse.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void triggerLogout() {
        // Clear session
        sessionManager.clearSession();
        
        // Notify listener on main thread
        if (authEventListener != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                authEventListener.onLogoutRequired();
            });
        }
    }
}
