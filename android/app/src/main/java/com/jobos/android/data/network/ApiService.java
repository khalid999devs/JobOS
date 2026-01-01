package com.jobos.android.data.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.shared.dto.auth.AuthResponse;
import com.jobos.shared.dto.auth.LoginRequest;
import com.jobos.shared.dto.auth.RegisterRequest;
import com.jobos.shared.dto.job.JobDTO;
import com.jobos.shared.dto.job.JobSearchRequest;
import com.jobos.shared.dto.job.CreateJobRequest;
import com.jobos.shared.dto.application.ApplicationDTO;
import com.jobos.shared.dto.application.CreateApplicationRequest;
import com.jobos.shared.dto.cv.CVDTO;
import com.jobos.shared.dto.cv.CreateCVRequest;
import com.jobos.shared.dto.cv.UpdateCVRequest;
import com.jobos.shared.dto.profile.ProfileResponse;
import com.jobos.shared.dto.profile.UpdateProfileRequest;
import com.jobos.shared.dto.notification.NotificationDTO;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

public class ApiService {

    private static final String BASE_URL = ApiConfig.getBaseUrl();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public ApiService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void login(LoginRequest request, ApiCallback<AuthResponse> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/auth/login")
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, AuthResponse.class, callback);
    }

    public void register(RegisterRequest request, ApiCallback<AuthResponse> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/auth/register")
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, AuthResponse.class, callback);
    }

    public void requestPasswordReset(String email, ApiCallback<String> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/auth/forgot-password")
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void resetPassword(String email, String otp, String password, ApiCallback<String> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("otp", otp);
        body.put("newPassword", password);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/auth/reset-password")
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void refreshToken(String refreshToken, ApiCallback<AuthResponse> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("refreshToken", refreshToken);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/auth/refresh")
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, AuthResponse.class, callback);
    }

    public void getProfile(String token, ApiCallback<ProfileResponse> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/profile")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, ProfileResponse.class, callback);
    }

    public void updateProfile(String token, UpdateProfileRequest request, ApiCallback<ProfileResponse> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/profile")
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, ProfileResponse.class, callback);
    }

    public void searchJobs(JobSearchRequest request, ApiCallback<List<JobDTO>> callback) {
        StringBuilder url = new StringBuilder(BASE_URL + "/api/jobs/search?");
        if (request.getKeyword() != null) url.append("keyword=").append(request.getKeyword()).append("&");
        if (request.getLocation() != null) url.append("location=").append(request.getLocation()).append("&");
        if (request.getJobType() != null) url.append("jobType=").append(request.getJobType()).append("&");
        if (request.getCategory() != null) url.append("category=").append(request.getCategory()).append("&");
        url.append("page=").append(request.getPage()).append("&");
        url.append("size=").append(request.getSize());

        Request httpRequest = new Request.Builder()
                .url(url.toString())
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<JobDTO>>() {}, callback);
    }

    public void getRecommendedJobs(String token, int page, int size, ApiCallback<List<JobDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/recommended?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<JobDTO>>() {}, callback);
    }

    public void getJobById(Long jobId, ApiCallback<JobDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId)
                .get()
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void createJob(String token, CreateJobRequest request, ApiCallback<JobDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void updateJob(String token, Long jobId, CreateJobRequest request, ApiCallback<JobDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId)
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void deleteJob(String token, Long jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void getMyPostedJobs(String token, int page, int size, ApiCallback<List<JobDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/my-jobs?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<JobDTO>>() {}, callback);
    }

    public void applyForJob(String token, CreateApplicationRequest request, ApiCallback<ApplicationDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, ApplicationDTO.class, callback);
    }

    public void getMyApplications(String token, int page, int size, ApiCallback<List<ApplicationDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/my-applications?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<ApplicationDTO>>() {}, callback);
    }

    public void getApplicationById(String token, Long applicationId, ApiCallback<ApplicationDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/" + applicationId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, ApplicationDTO.class, callback);
    }

    public void getApplicationsForJob(String token, Long jobId, int page, int size, ApiCallback<List<ApplicationDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/job/" + jobId + "?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<ApplicationDTO>>() {}, callback);
    }

    public void updateApplicationStatus(String token, Long applicationId, String status, ApiCallback<ApplicationDTO> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("status", status);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, ApplicationDTO.class, callback);
    }

    public void getMyCVs(String token, ApiCallback<List<CVDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<CVDTO>>() {}, callback);
    }

    public void getCVById(String token, Long cvId, ApiCallback<CVDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void createCV(String token, CreateCVRequest request, ApiCallback<CVDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void updateCV(String token, Long cvId, UpdateCVRequest request, ApiCallback<CVDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void deleteCV(String token, Long cvId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void saveJob(String token, Long jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/saved-jobs/" + jobId)
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create("", JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void unsaveJob(String token, Long jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/saved-jobs/" + jobId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void getSavedJobs(String token, int page, int size, ApiCallback<List<JobDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/saved-jobs?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<JobDTO>>() {}, callback);
    }

    public void getNotifications(String token, int page, int size, ApiCallback<List<NotificationDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<NotificationDTO>>() {}, callback);
    }

    public void markNotificationRead(String token, Long notificationId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create("", JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void markAllNotificationsRead(String token, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications/read-all")
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create("", JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void registerFcmToken(String token, String fcmToken, ApiCallback<String> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("token", fcmToken);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications/fcm-token")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    private <T> void executeAsync(Request request, Class<T> responseType, ApiCallback<T> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        T result = objectMapper.readValue(body, responseType);
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    private <T> void executeAsyncList(Request request, TypeReference<T> typeRef, ApiCallback<T> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        T result = objectMapper.readValue(body, typeRef);
                        callback.onSuccess(result);
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    private void executeAsyncString(Request request, ApiCallback<String> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    callback.onSuccess(body);
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    private String parseError(String body) {
        try {
            Map<String, Object> error = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
            if (error.containsKey("message")) {
                return error.get("message").toString();
            }
        } catch (Exception ignored) {}
        return "An error occurred";
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
