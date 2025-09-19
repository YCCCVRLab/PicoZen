package com.barnabwhy.picozen.network;

import android.content.Context;
import android.util.Log;

import com.barnabwhy.picozen.utils.FileSizeUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Network helper class for PicoZen VR App Store
 * Now configured for Vercel deployment instead of Netlify
 * Fixes "couldn't fetch files" and file size calculation issues
 */
public class NetworkHelper {
    
    private static final String TAG = "NetworkHelper";
    private static final int CONNECT_TIMEOUT = 30; // seconds
    private static final int READ_TIMEOUT = 60; // seconds
    private static final int WRITE_TIMEOUT = 60; // seconds
    
    // Updated for Vercel deployment - replace with your actual Vercel URL
    public static final String DEFAULT_API_SERVER = "https://picozen-server.vercel.app";
    public static final String FALLBACK_SERVER = "https://ycccrlab.github.io/PicoZen-Web";
    
    private OkHttpClient client;
    private Gson gson;
    private String serverUrl;
    
    public NetworkHelper() {
        this(DEFAULT_API_SERVER);
    }
    
    public NetworkHelper(String serverUrl) {
        this.serverUrl = serverUrl;
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .addHeader("User-Agent", "PicoZen-Android/0.7.6-yccc-vercel")
                            .addHeader("Accept", "application/json")
                            .addHeader("Cache-Control", "no-cache")
                            .addHeader("X-Requested-With", "PicoZen-VR");
                    Request request = requestBuilder.build();
                    return chain.proceed(request);
                })
                .build();
    }
    
    /**
     * Interface for network callbacks
     */
    public interface NetworkCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    /**
     * Interface for file size callbacks
     */
    public interface FileSizeCallback {
        void onFileSizeDetected(long sizeInBytes);
        void onError(String error);
    }
    
    /**
     * Test server connectivity - now optimized for Vercel
     */
    public void testConnection(NetworkCallback callback) {
        String healthUrl = serverUrl + "/api/health";
        
        Log.d(TAG, "Testing Vercel server connection: " + healthUrl);
        
        Request request = new Request.Builder()
                .url(healthUrl)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Health check failed: " + e.getMessage());
                
                // Try test endpoint as fallback
                tryTestEndpoint(callback);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "✅ Vercel server is healthy: " + responseBody);
                        
                        // Parse response to verify it's our server
                        JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
                        if (json.has("status") && "ok".equals(json.get("status").getAsString())) {
                            String serverType = json.has("server") ? json.get("server").getAsString() : "Unknown";
                            callback.onSuccess("Connected to " + serverType);
                        } else {
                            callback.onError("Invalid server response");
                        }
                    } else {
                        Log.w(TAG, "Health check returned " + response.code());
                        tryTestEndpoint(callback);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing health response", e);
                    tryTestEndpoint(callback);
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Try test endpoint if health check fails (handles Vercel cold starts)
     */
    private void tryTestEndpoint(NetworkCallback callback) {
        String testUrl = serverUrl + "/api/test";
        
        Log.d(TAG, "Trying test endpoint (cold start?): " + testUrl);
        
        Request request = new Request.Builder()
                .url(testUrl)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Test endpoint also failed: " + e.getMessage());
                callback.onError("Server unavailable: " + e.getMessage() + 
                                "\\n\\nTip: Server may be starting up (cold start). Try again in a few seconds.");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "✅ Test endpoint successful: " + responseBody);
                        callback.onSuccess("Server connected (was cold starting)");
                    } else {
                        callback.onError("Server error: HTTP " + response.code());
                    }
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Fetch apps from Vercel server with proper error handling
     */
    public void fetchApps(NetworkCallback callback) {
        String appsUrl = serverUrl + "/api/apps";
        
        Log.d(TAG, "Fetching apps from Vercel: " + appsUrl);
        
        Request request = new Request.Builder()
                .url(appsUrl)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch apps: " + e.getMessage());
                callback.onError("Couldn't fetch files: " + e.getMessage() + 
                                "\\n\\nPossible causes:\\n" +
                                "• Server is cold starting (try again)\\n" +
                                "• Network connectivity issues\\n" +
                                "• Server URL needs updating");
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String responseBody = response.body().string();
                        Log.d(TAG, "✅ Apps fetched successfully");
                        
                        // Validate and fix file sizes before returning
                        String processedResponse = validateAndFixFileSizes(responseBody);
                        callback.onSuccess(processedResponse);
                    } else {
                        String errorBody = "";
                        try {
                            errorBody = response.body().string();
                        } catch (Exception ignored) {}
                        
                        Log.w(TAG, "Apps API returned " + response.code() + ": " + errorBody);
                        callback.onError("Server error: HTTP " + response.code() + 
                                        (errorBody.isEmpty() ? "" : "\\n" + errorBody));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing apps response", e);
                    callback.onError("Error processing server response: " + e.getMessage());
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Validate and fix file sizes to prevent the "637.80 MB / 150.00 MB (425.20%)" bug
     */
    private String validateAndFixFileSizes(String responseBody) {
        try {
            JsonObject response = JsonParser.parseString(responseBody).getAsJsonObject();
            
            if (response.has("apps") && response.get("apps").isJsonArray()) {
                response.get("apps").getAsJsonArray().forEach(appElement -> {
                    if (appElement.isJsonObject()) {
                        JsonObject app = appElement.getAsJsonObject();
                        
                        // Fix file size calculation issues
                        if (app.has("fileSize")) {
                            long fileSize = app.get("fileSize").getAsLong();
                            
                            // Validate file size is reasonable (between 1KB and 10GB)
                            if (fileSize < 1024 || fileSize > 10L * 1024 * 1024 * 1024) {
                                Log.w(TAG, "Suspicious file size: " + fileSize + " bytes for " + 
                                      (app.has("title") ? app.get("title").getAsString() : "unknown app"));
                                
                                // Try to get size from formatted string
                                if (app.has("fileSizeFormatted")) {
                                    String formattedSize = app.get("fileSizeFormatted").getAsString();
                                    long parsedSize = FileSizeUtils.parseFileSize(formattedSize);
                                    if (parsedSize > 0 && parsedSize < 10L * 1024 * 1024 * 1024) {
                                        app.addProperty("fileSize", parsedSize);
                                        Log.d(TAG, "Fixed file size: " + FileSizeUtils.formatFileSize(parsedSize));
                                    }
                                }
                            }
                        }
                        
                        // Ensure fileSizeFormatted exists
                        if (app.has("fileSize") && !app.has("fileSizeFormatted")) {
                            long fileSize = app.get("fileSize").getAsLong();
                            if (fileSize > 0) {
                                app.addProperty("fileSizeFormatted", FileSizeUtils.formatFileSize(fileSize));
                            }
                        }
                    }
                });
            }
            
            return response.toString();
        } catch (Exception e) {
            Log.w(TAG, "Could not validate file sizes, returning original response", e);
            return responseBody;
        }
    }
    
    /**
     * Get file size from download URL with Vercel optimization
     */
    public void getFileSize(String downloadUrl, FileSizeCallback callback) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            callback.onError("Invalid download URL");
            return;
        }
        
        Log.d(TAG, "Getting file size for: " + downloadUrl);
        
        Request request = new Request.Builder()
                .url(downloadUrl)
                .head() // Use HEAD request for efficiency
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to get file size: " + e.getMessage());
                callback.onError("Unable to determine file size: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String contentLength = response.header("Content-Length");
                    if (contentLength != null && !contentLength.isEmpty()) {
                        try {
                            long size = Long.parseLong(contentLength);
                            Log.d(TAG, "✅ File size detected: " + FileSizeUtils.formatFileSize(size));
                            callback.onFileSizeDetected(size);
                        } catch (NumberFormatException e) {
                            callback.onError("Invalid file size from server: " + contentLength);
                        }
                    } else {
                        Log.w(TAG, "No Content-Length header found");
                        callback.onError("Server did not provide file size information");
                    }
                } finally {
                    response.close();
                }
            }
        });
    }
    
    /**
     * Update server URL (useful for switching between environments)
     */
    public void setServerUrl(String url) {
        this.serverUrl = url;
        Log.d(TAG, "Server URL updated to: " + url);
    }
    
    /**
     * Get current server URL
     */
    public String getServerUrl() {
        return serverUrl;
    }
    
    /**
     * Check if using Vercel server
     */
    public boolean isUsingVercel() {
        return serverUrl.contains("vercel.app");
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
    }
}