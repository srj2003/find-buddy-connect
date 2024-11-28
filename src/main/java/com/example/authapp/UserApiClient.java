package com.example.authapp;
import okhttp3.*;
import java.io.IOException;
public class UserApiClient {
    private static final String API_BASE_URL = "https://my-flask-api-1s94.onrender.com";
    private OkHttpClient client = new OkHttpClient();
    public void sendUIDToServer(String uid, final ApiCallback callback) {
        String url = API_BASE_URL + "/user?uid=" + uid;
        // Create a GET request
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        // Execute the request asynchronously
        client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            callback.onFailure("Network error: " + e.getMessage());
        }
        @Override
        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
            callback.onSuccess(response.body().string());
        } else {
            callback.onFailure("Server error: " + response.code());                }
        }
        });
}

    // Callback interface
    public interface ApiCallback {
    void onSuccess(String response);
    void onFailure(String error);
}}