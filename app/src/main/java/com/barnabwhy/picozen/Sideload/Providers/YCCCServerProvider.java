package com.barnabwhy.picozen.Sideload.Providers;

import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;
import android.view.View;

import com.barnabwhy.picozen.MainActivity;
import com.barnabwhy.picozen.SettingsProvider;
import com.barnabwhy.picozen.Sideload.SideloadItem;
import com.barnabwhy.picozen.Sideload.SideloadItemType;
import com.barnabwhy.picozen.SideloadAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class YCCCServerProvider extends AbstractProvider {
    // Updated to use new Koyeb deployment server
    private static final String SERVER_BASE_URL = "https://above-odella-john-barr-40e8cdf4.koyeb.app/api";
    private static final String TAG = "YCCCServerProvider";
    
    // Store additional metadata for each item
    private Map<String, String> itemDescriptions = new HashMap<>();
    private Map<String, String> itemDownloadUrls = new HashMap<>();

    @Override
    public boolean usesAddress() {
        return false; // We use our fixed server, no manual address needed
    }

    public YCCCServerProvider(SharedPreferences sharedPreferences, MainActivity mainActivityContext, Runnable notifyCallback) {
        super(sharedPreferences, mainActivityContext, notifyCallback);
        state = ProviderState.IDLE;
        updateList();
    }

    @Override
    public void setHolder(int position, SideloadAdapter.ViewHolder holder) {
        SideloadItem item = getItem(position);
        holder.name.setText(item.getName());
        
        // Set description if available
        String description = itemDescriptions.get(item.getName());
        if (description != null && description.length() > 100) {
            holder.modified.setText(description.substring(0, 97) + "...");
        } else {
            holder.modified.setText(item.getModifiedAt());
        }
        
        // Show download icon for files
        if (item.getType() == SideloadItemType.FILE) {
            holder.downloadIcon.setVisibility(View.VISIBLE);
            holder.openFolderIcon.setVisibility(View.GONE);
        } else {
            holder.downloadIcon.setVisibility(View.GONE);
            holder.openFolderIcon.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void updateList() {
        Thread thread = new Thread(() -> {
            itemList = getAppsFromServer();
            Log.i(TAG, "Loaded " + itemList.size() + " apps from YCCC server");
            mainActivityContext.ensureStoragePermissions();
            mainActivityContext.runOnUiThread(notifyCallback);
        });
        thread.start();
    }

    private ArrayList<SideloadItem> getAppsFromServer() {
        ArrayList<SideloadItem> items = new ArrayList<>();
        
        try {
            Log.i(TAG, "Connecting to YCCC VR Lab server...");
            
            // Fetch apps from the API
            URL appsUrl = new URL(SERVER_BASE_URL + "/apps");
            InputStream appsStream = appsUrl.openStream();
            Reader appsReader = new InputStreamReader(appsStream, StandardCharsets.UTF_8);
            
            StringBuilder appsJson = new StringBuilder();
            char[] buffer = new char[1024];
            int bytesRead;
            while ((bytesRead = appsReader.read(buffer)) != -1) {
                appsJson.append(buffer, 0, bytesRead);
            }
            appsReader.close();
            
            Log.i(TAG, "Received response from server: " + appsJson.toString().substring(0, Math.min(200, appsJson.length())));
            
            JSONObject response = new JSONObject(appsJson.toString());
            
            if (response.getBoolean("success")) {
                JSONArray appsArray = response.getJSONArray("apps");
                
                Log.i(TAG, "Found " + appsArray.length() + " apps on server");
                
                for (int i = 0; i < appsArray.length(); i++) {
                    JSONObject app = appsArray.getJSONObject(i);
                    
                    String title = app.getString("title");
                    String description = app.optString("shortDescription", app.optString("description", ""));
                    String developer = app.optString("developer", "Unknown");
                    String version = app.optString("version", "1.0");
                    String category = app.optString("category", "Apps");
                    double rating = app.optDouble("rating", 0.0);
                    int downloadCount = app.optInt("downloadCount", 0);
                    String fileSizeFormatted = app.optString("fileSizeFormatted", "Unknown");
                    long fileSize = app.optLong("fileSize", 0);
                    
                    // Create download URL
                    String downloadUrl = SERVER_BASE_URL + "/download/" + app.getInt("id");
                    
                    // Create detailed description
                    StringBuilder detailedDescription = new StringBuilder();
                    detailedDescription.append("📱 ").append(title).append("\n");
                    detailedDescription.append("👨‍💻 Developer: ").append(developer).append("\n");
                    detailedDescription.append("🏷️ Version: ").append(version).append("\n");
                    detailedDescription.append("📂 Category: ").append(category).append("\n");
                    detailedDescription.append("⭐ Rating: ").append(String.format("%.1f", rating)).append("/5\n");
                    detailedDescription.append("⬇️ Downloads: ").append(downloadCount).append("\n");
                    detailedDescription.append("📦 Size: ").append(fileSizeFormatted).append("\n\n");
                    detailedDescription.append("📝 Description:\n").append(description);
                    
                    // Create sideload item using proper constructor
                    SideloadItem item = new SideloadItem(
                        SideloadItemType.FILE,
                        title,
                        downloadUrl, // Use download URL as path
                        fileSize > 0 ? fileSize : 50000000, // Use actual file size or default
                        "" // No modification date from server
                    );
                    
                    // Store additional metadata
                    itemDescriptions.put(title, detailedDescription.toString());
                    itemDownloadUrls.put(title, downloadUrl);
                    
                    items.add(item);
                    
                    Log.d(TAG, "Added app: " + title + " (" + fileSizeFormatted + ")");
                }
            } else {
                Log.e(TAG, "Server returned error: " + response.optString("error", "Unknown error"));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch apps from YCCC server", e);
            
            // Add a fallback item to show connection status
            SideloadItem errorItem = new SideloadItem(
                SideloadItemType.DIRECTORY, // Use DIRECTORY instead of FOLDER
                "❌ Connection Error",
                "",
                0,
                ""
            );
            
            String errorDescription = "Failed to connect to YCCC VR Lab server.\n\n" +
                    "Error: " + e.getMessage() + "\n\n" +
                    "Please check your internet connection and try again.\n\n" +
                    "Server: " + SERVER_BASE_URL;
                    
            itemDescriptions.put("❌ Connection Error", errorDescription);
            items.add(errorItem);
        }
        
        return items;
    }

    @Override
    public void downloadFile(SideloadItem item, Consumer<File> startCallback, Consumer<Long> progressCallback, Consumer<File> completeCallback, Consumer<Exception> errorCallback) {
        String downloadUrl = itemDownloadUrls.get(item.getName());
        
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            Log.e(TAG, "No download URL for item: " + item.getName());
            errorCallback.accept(new Exception("No download URL available"));
            return;
        }

        Thread downloadThread = new Thread(() -> {
            try {
                Log.i(TAG, "Starting download: " + item.getName() + " from " + downloadUrl);
                
                URL url = new URL(downloadUrl);
                InputStream inputStream = url.openStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                
                // Create downloads directory if it doesn't exist
                File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PicoZen");
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs();
                }
                
                // Generate filename
                String filename = item.getName().replaceAll("[^a-zA-Z0-9._-]", "_") + ".apk";
                File outputFile = new File(downloadsDir, filename);
                
                // Call start callback
                startCallback.accept(outputFile);
                
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    // Update progress
                    progressCallback.accept(totalBytesRead);
                }
                
                fileOutputStream.close();
                dataInputStream.close();
                
                Log.i(TAG, "Download completed: " + filename + " (" + totalBytesRead + " bytes)");
                
                // Call completion callback
                completeCallback.accept(outputFile);
                
            } catch (Exception e) {
                Log.e(TAG, "Download failed for " + item.getName(), e);
                errorCallback.accept(e);
            }
        });
        
        downloadThread.start();
    }

    public void openFolder(SideloadItem item) {
        // For server items, show description instead of opening folder
        String description = itemDescriptions.get(item.getName());
        if (description != null) {
            // You can implement a dialog or toast to show the description
            Log.i(TAG, "Item description: " + description);
        }
    }

    public void goBack() {
        // Not applicable for server provider
        Log.d(TAG, "goBack called on server provider - not applicable");
    }

    public boolean canGoBack() {
        return false; // Server provider doesn't have folder navigation
    }

    public String getCurrentPath() {
        return "YCCC VR Lab Server";
    }

    public void setCredentials(String username, String password) {
        // Not needed for our server
    }

    public View getSettingsView() {
        // No additional settings needed for YCCC server
        return null;
    }

    public String getProviderName() {
        return "YCCC VR Lab Server";
    }

    public String getProviderDescription() {
        return "Connect to the York County Community College VR Lab app store for curated educational VR applications.";
    }
    
    // Get description for an item
    public String getItemDescription(String itemName) {
        return itemDescriptions.get(itemName);
    }
    
    // Get download URL for an item  
    public String getItemDownloadUrl(String itemName) {
        return itemDownloadUrls.get(itemName);
    }
}