package com.jobos.android.data.network;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobos.android.config.ApiConfig;
import com.jobos.android.data.model.auth.AuthResponse;
import com.jobos.android.data.model.auth.LoginRequest;
import com.jobos.android.data.model.auth.RegisterRequest;
import com.jobos.android.data.model.job.JobDTO;
import com.jobos.android.data.model.job.JobSearchRequest;
import com.jobos.android.data.model.job.CreateJobRequest;
import com.jobos.android.data.model.application.ApplicationDTO;
import com.jobos.android.data.model.application.CreateApplicationRequest;
import com.jobos.android.data.model.cv.CVDTO;
import com.jobos.android.data.model.cv.CreateCVRequest;
import com.jobos.android.data.model.cv.UpdateCVRequest;
import com.jobos.android.data.model.profile.ProfileResponse;
import com.jobos.android.data.model.profile.UpdateProfileRequest;
import com.jobos.android.data.model.notification.NotificationDTO;
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
                .url(BASE_URL + "/api/users/me")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, ProfileResponse.class, callback);
    }

    public void updateProfile(String token, UpdateProfileRequest request, ApiCallback<ProfileResponse> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/users/me")
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, ProfileResponse.class, callback);
    }

    public void searchJobs(JobSearchRequest request, ApiCallback<List<JobDTO>> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/search")
                .post(RequestBody.create(json, JSON))
                .build();

        // Backend returns JobSearchResponse with jobs list
        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        // Parse JobSearchResponse and extract jobs list
                        Map<String, Object> searchResponse = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Object jobsObj = searchResponse.get("jobs");
                        if (jobsObj != null) {
                            String jobsJson = objectMapper.writeValueAsString(jobsObj);
                            List<JobDTO> jobs = objectMapper.readValue(jobsJson, new TypeReference<List<JobDTO>>() {});
                            callback.onSuccess(jobs);
                        } else {
                            callback.onSuccess(new java.util.ArrayList<>());
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    public void getRecommendedJobs(String token, int page, int size, ApiCallback<List<JobDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/recommended?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsyncList(httpRequest, new TypeReference<List<JobDTO>>() {}, callback);
    }

    public void getJobById(String jobId, ApiCallback<JobDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId)
                .get()
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void getJobDetails(String token, String jobId, ApiCallback<JobDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void createJob(String token, CreateJobRequest request, ApiCallback<JobDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void createJob(String token, Map<String, Object> jobData, ApiCallback<JobDTO> callback) {
        String json = toJson(jobData);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void updateJob(String token, String jobId, CreateJobRequest request, ApiCallback<JobDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts/" + jobId)
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void updateJob(String token, String jobId, Map<String, Object> jobData, ApiCallback<JobDTO> callback) {
        String json = toJson(jobData);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts/" + jobId)
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, JobDTO.class, callback);
    }

    public void deleteJob(String token, String jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts/" + jobId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void deleteJob(String token, String jobId, ApiCallback<Void> callback, boolean ignoreResponse) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts/" + jobId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncVoid(httpRequest, callback);
    }

    public void getMyPostedJobs(String token, int page, int size, ApiCallback<List<JobDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        // Backend returns paginated response with jobs list
        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        Map<String, Object> pageResponse = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Object jobsObj = pageResponse.get("jobs");
                        if (jobsObj != null) {
                            String jobsJson = objectMapper.writeValueAsString(jobsObj);
                            List<JobDTO> jobs = objectMapper.readValue(jobsJson, new TypeReference<List<JobDTO>>() {});
                            callback.onSuccess(jobs);
                        } else {
                            callback.onSuccess(new java.util.ArrayList<>());
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    public void getMyPostedJobs(String token, ApiCallback<List<JobDTO>> callback) {
        getMyPostedJobs(token, 0, 100, callback);
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

    public void getApplicationById(String token, String applicationId, ApiCallback<ApplicationDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/" + applicationId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, ApplicationDTO.class, callback);
    }

    public void getApplicationsForJob(String token, String jobId, int page, int size, ApiCallback<List<ApplicationDTO>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/job-posts/" + jobId + "/applicants?page=" + page + "&size=" + size)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        // Backend returns paginated response with applicants list
        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        Map<String, Object> pageResponse = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Object applicantsObj = pageResponse.get("applicants");
                        if (applicantsObj != null) {
                            String applicantsJson = objectMapper.writeValueAsString(applicantsObj);
                            List<ApplicationDTO> applications = objectMapper.readValue(applicantsJson, new TypeReference<List<ApplicationDTO>>() {});
                            callback.onSuccess(applications);
                        } else {
                            callback.onSuccess(new java.util.ArrayList<>());
                        }
                    } catch (Exception e) {
                        callback.onError("Parse error: " + e.getMessage());
                    }
                } else {
                    callback.onError(parseError(body));
                }
            }
        });
    }

    public void getJobApplications(String token, String jobId, ApiCallback<List<ApplicationDTO>> callback) {
        getApplicationsForJob(token, jobId, 0, 100, callback);
    }

    public void getApplicationDetails(String token, String applicationId, ApiCallback<ApplicationDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/" + applicationId)
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        executeAsync(httpRequest, ApplicationDTO.class, callback);
    }

    public void updateApplicationStatus(String token, String applicationId, String status, ApiCallback<ApplicationDTO> callback) {
        Map<String, String> body = new HashMap<>();
        body.put("status", status);
        String json = toJson(body);

        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/applications/" + applicationId + "/status")
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
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

    public void getCVById(String token, String cvId, ApiCallback<CVDTO> callback) {
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

    public void createCV(String token, Map<String, Object> cvData, ApiCallback<CVDTO> callback) {
        String json = toJson(cvData);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void updateCV(String token, String cvId, UpdateCVRequest request, ApiCallback<CVDTO> callback) {
        String json = toJson(request);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void updateCV(String token, String cvId, Map<String, Object> cvData, ApiCallback<CVDTO> callback) {
        String json = toJson(cvData);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create(json, JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void getCVDetails(String token, String cvId, ApiCallback<CVDTO> callback) {
        getCVById(token, cvId, callback);
    }

    public void deleteCV(String token, String cvId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId)
                .header("Authorization", "Bearer " + token)
                .delete()
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void saveJob(String token, String jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId + "/save")
                .header("Authorization", "Bearer " + token)
                .post(RequestBody.create("", JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void unsaveJob(String token, String jobId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/jobs/" + jobId + "/unsave")
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

    public void markNotificationRead(String token, String notificationId, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications/" + notificationId + "/read")
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create("", JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void markAllNotificationsRead(String token, ApiCallback<String> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/notifications/read-all")
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create("", JSON))
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

    public void changePassword(String token, Map<String, String> passwordData, ApiCallback<String> callback) {
        String json = toJson(passwordData);
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/profile/change-password")
                .header("Authorization", "Bearer " + token)
                .put(RequestBody.create(json, JSON))
                .build();

        executeAsyncString(httpRequest, callback);
    }

    public void setDefaultCV(String token, String cvId, ApiCallback<CVDTO> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/cvs/" + cvId + "/set-default")
                .header("Authorization", "Bearer " + token)
                .patch(RequestBody.create("", JSON))
                .build();

        executeAsync(httpRequest, CVDTO.class, callback);
    }

    public void getProfileStats(String token, ApiCallback<Map<String, Object>> callback) {
        Request httpRequest = new Request.Builder()
                .url(BASE_URL + "/api/profile/stats")
                .header("Authorization", "Bearer " + token)
                .get()
                .build();

        client.newCall(httpRequest).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    try {
                        Map<String, Object> result = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
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
                        Map<String, Object> apiResponse = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Object resultObj = apiResponse.get("result");
                        if (resultObj != null) {
                            String resultJson = objectMapper.writeValueAsString(resultObj);
                            T result = objectMapper.readValue(resultJson, responseType);
                            callback.onSuccess(result);
                        } else {
                            T result = objectMapper.readValue(body, responseType);
                            callback.onSuccess(result);
                        }
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
                        Map<String, Object> apiResponse = objectMapper.readValue(body, new TypeReference<Map<String, Object>>() {});
                        Object resultObj = apiResponse.get("result");
                        if (resultObj != null) {
                            String resultJson = objectMapper.writeValueAsString(resultObj);
                            T result = objectMapper.readValue(resultJson, typeRef);
                            callback.onSuccess(result);
                        } else {
                            T result = objectMapper.readValue(body, typeRef);
                            callback.onSuccess(result);
                        }
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

    private void executeAsyncVoid(Request request, ApiCallback<Void> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                if (response.isSuccessful()) {
                    callback.onSuccess(null);
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
