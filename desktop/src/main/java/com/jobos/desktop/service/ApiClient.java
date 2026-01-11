package com.jobos.desktop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jobos.desktop.core.config.AppConfig;
import com.jobos.desktop.core.config.Constants;
import com.jobos.desktop.core.navigation.Router;
import com.jobos.desktop.core.navigation.Route;
import com.jobos.desktop.core.session.SessionManager;
import com.jobos.desktop.core.session.TokenStore;
import com.jobos.desktop.util.JsonUtil;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.common.ApiResponse;
import javafx.application.Platform;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    
    private static ApiClient instance;
    private final OkHttpClient client;
    private final String baseUrl;
    private final TokenStore tokenStore;
    private volatile boolean isRefreshing = false;
    private final List<Runnable> pendingRequests = new ArrayList<>();
    
    private ApiClient() {
        this.baseUrl = AppConfig.getInstance().getApiBaseUrl();
        this.tokenStore = TokenStore.getInstance();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }
    
    public <T> CompletableFuture<T> get(String endpoint, Class<T> responseType) {
        return executeRequest(buildRequest(endpoint, "GET", null), responseType, false);
    }
    
    public <T> CompletableFuture<T> get(String endpoint, TypeReference<T> typeRef) {
        return executeRequest(buildRequest(endpoint, "GET", null), typeRef, false);
    }
    
    public <T> CompletableFuture<T> post(String endpoint, Object body, Class<T> responseType) {
        return executeRequest(buildRequest(endpoint, "POST", body), responseType, false);
    }
    
    public <T> CompletableFuture<T> post(String endpoint, Object body, TypeReference<T> typeRef) {
        return executeRequest(buildRequest(endpoint, "POST", body), typeRef, false);
    }
    
    public <T> CompletableFuture<T> patch(String endpoint, Object body, Class<T> responseType) {
        return executeRequest(buildRequest(endpoint, "PATCH", body), responseType, false);
    }
    
    public <T> CompletableFuture<T> patch(String endpoint, Object body, TypeReference<T> typeRef) {
        return executeRequest(buildRequest(endpoint, "PATCH", body), typeRef, false);
    }
    
    public <T> CompletableFuture<T> put(String endpoint, Object body, Class<T> responseType) {
        return executeRequest(buildRequest(endpoint, "PUT", body), responseType, false);
    }
    
    public CompletableFuture<Void> delete(String endpoint) {
        return executeRequest(buildRequest(endpoint, "DELETE", null), Void.class, false)
                .thenApply(v -> null);
    }
    
    public <T> CompletableFuture<T> delete(String endpoint, TypeReference<T> typeRef) {
        return executeRequest(buildRequest(endpoint, "DELETE", null), typeRef, false);
    }
    
    private Request buildRequest(String endpoint, String method, Object body) {
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + endpoint)
                .header("Content-Type", "application/json");
        
        String accessToken = tokenStore.getAccessToken();
        if (accessToken != null && !accessToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        
        RequestBody requestBody = null;
        if (body != null) {
            requestBody = RequestBody.create(
                    JsonUtil.toJson(body),
                    MediaType.parse("application/json")
            );
        }
        
        switch (method) {
            case "GET" -> builder.get();
            case "POST" -> builder.post(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            case "PATCH" -> builder.patch(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            case "PUT" -> builder.put(requestBody != null ? requestBody : RequestBody.create("", MediaType.parse("application/json")));
            case "DELETE" -> builder.delete(requestBody);
        }
        
        return builder.build();
    }
    
    private <T> CompletableFuture<T> executeRequest(Request request, Class<T> responseType, boolean isRetry) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ApiException("Network error: " + e.getMessage(), 0));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(request, response, responseType, null, isRetry, future);
            }
        });
        
        return future;
    }
    
    private <T> CompletableFuture<T> executeRequest(Request request, TypeReference<T> typeRef, boolean isRetry) {
        CompletableFuture<T> future = new CompletableFuture<>();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(new ApiException("Network error: " + e.getMessage(), 0));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(request, response, null, typeRef, isRetry, future);
            }
        });
        
        return future;
    }
    
    private <T> void handleResponse(Request originalRequest, Response response, Class<T> responseType, 
                                     TypeReference<T> typeRef, boolean isRetry, CompletableFuture<T> future) {
        try {
            String responseBody = response.body() != null ? response.body().string() : "";
            
          
            boolean isAuthEndpoint = isAuthEndpoint(originalRequest);
            if (response.code() == 401 && !isRetry && !isAuthEndpoint) {
                response.close();
                tryRefreshAndRetry(originalRequest, responseType, typeRef, future);
                return;
            }
            
            if (!response.isSuccessful()) {
                String errorMessage = extractErrorMessage(responseBody);
                future.completeExceptionally(new ApiException(errorMessage, response.code()));
                return;
            }
            
            if (responseType == Void.class || responseBody.isEmpty()) {
                future.complete(null);
                return;
            }
            
            T result;
            if (responseType != null) {
                result = JsonUtil.fromJson(responseBody, responseType);
            } else {
                result = JsonUtil.fromJson(responseBody, typeRef);
            }
            future.complete(result);
            
        } catch (Exception e) {
            future.completeExceptionally(new ApiException("Failed to parse response: " + e.getMessage(), response.code()));
        } finally {
            response.close();
        }
    }

    private boolean isAuthEndpoint(Request request) {
        String url = request.url().toString();
        return url.contains("/api/auth/login") || 
               url.contains("/api/auth/register") || 
               url.contains(Constants.Api.AUTH_REFRESH);
    }

    private boolean isRefreshEndpoint(Request request) {
        return request.url().toString().contains(Constants.Api.AUTH_REFRESH);
    }
    
    private <T> void tryRefreshAndRetry(Request originalRequest, Class<T> responseType, 
                                         TypeReference<T> typeRef, CompletableFuture<T> future) {
        String refreshToken;
        
        synchronized (this) {
            if (isRefreshing) {
                // Queue this request to be retried after refresh completes
                pendingRequests.add(() -> {
                    if (responseType != null) {
                        executeRequest(originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + tokenStore.getAccessToken())
                                .build(), responseType, true)
                                .thenAccept(future::complete)
                                .exceptionally(e -> {
                                    future.completeExceptionally(e);
                                    return null;
                                });
                    } else {
                        executeRequest(originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + tokenStore.getAccessToken())
                                .build(), typeRef, true)
                                .thenAccept(future::complete)
                                .exceptionally(e -> {
                                    future.completeExceptionally(e);
                                    return null;
                                });
                    }
                });
                return;
            }
            
            refreshToken = tokenStore.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                handleSessionExpired();
                future.completeExceptionally(new ApiException("Session expired", 401));
                return;
            }
            
            isRefreshing = true;
        }
        
        Map<String, String> refreshBody = Map.of("refreshToken", refreshToken);
        Request refreshRequest = new Request.Builder()
                .url(baseUrl + Constants.Api.AUTH_REFRESH)
                .post(RequestBody.create(JsonUtil.toJson(refreshBody), MediaType.parse("application/json")))
                .header("Content-Type", "application/json")
                .build();
        
        client.newCall(refreshRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                synchronized (ApiClient.this) {
                    isRefreshing = false;
                    pendingRequests.clear();
                }
                handleSessionExpired();
                future.completeExceptionally(new ApiException("Session expired", 401));
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    response.close();
                    synchronized (ApiClient.this) {
                        isRefreshing = false;
                        pendingRequests.clear();
                    }
                    handleSessionExpired();
                    future.completeExceptionally(new ApiException("Session expired", 401));
                    return;
                }
                
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    response.close();
                    
                    ApiResponse<AuthResponse> apiResponse = JsonUtil.fromJson(body, 
                            new TypeReference<ApiResponse<AuthResponse>>() {});
                    
                    if (apiResponse != null && apiResponse.getResult() != null) {
                        AuthResponse authResponse = apiResponse.getResult();
                        tokenStore.saveTokens(authResponse.getAccessToken(), authResponse.getRefreshToken());
                        
                        // Retry the original request
                        Request retryRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + authResponse.getAccessToken())
                                .build();
                        
                        if (responseType != null) {
                            executeRequest(retryRequest, responseType, true)
                                    .thenAccept(future::complete)
                                    .exceptionally(e -> {
                                        future.completeExceptionally(e);
                                        return null;
                                    });
                        } else {
                            executeRequest(retryRequest, typeRef, true)
                                    .thenAccept(future::complete)
                                    .exceptionally(e -> {
                                        future.completeExceptionally(e);
                                        return null;
                                    });
                        }
                        
                        // Process all pending requests
                        List<Runnable> toProcess;
                        synchronized (ApiClient.this) {
                            toProcess = new ArrayList<>(pendingRequests);
                            pendingRequests.clear();
                            isRefreshing = false;
                        }
                        toProcess.forEach(Runnable::run);
                    } else {
                        synchronized (ApiClient.this) {
                            isRefreshing = false;
                            pendingRequests.clear();
                        }
                        handleSessionExpired();
                        future.completeExceptionally(new ApiException("Session expired", 401));
                    }
                } catch (Exception e) {
                    synchronized (ApiClient.this) {
                        isRefreshing = false;
                        pendingRequests.clear();
                    }
                    handleSessionExpired();
                    future.completeExceptionally(new ApiException("Session expired", 401));
                }
            }
        });
    }
    
    private void handleSessionExpired() {
        Platform.runLater(() -> {
            SessionManager.getInstance().logout();
            Router.getInstance().navigate(Route.WELCOME);
        });
    }
    
    private String extractErrorMessage(String responseBody) {
        try {
            ApiResponse<?> response = JsonUtil.fromJson(responseBody, ApiResponse.class);
            if (response != null && response.getMessage() != null) {
                return response.getMessage();
            }
        } catch (Exception ignored) {}
        return "Request failed";
    }
    
    public static class ApiException extends Exception {
        private final int statusCode;
        
        public ApiException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }
}
