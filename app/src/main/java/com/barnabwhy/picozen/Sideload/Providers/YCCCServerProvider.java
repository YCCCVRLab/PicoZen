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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

public class YCCCServerProvider extends AbstractProvider {
    private static final String SERVER_BASE_URL = "https://picozen-api.netlify.app/api";
    private static final String TAG = "YCCCServerProvider";

    @Override
    public boolean usesAddress() {
        return false; // We use our fixed server, no manual address needed
    }

    public YCCCServerProvider(SharedPreferences sharedPreferences, MainActivity mainActivityContext, Runnable notifyCallback) {
        super(sharedPreferences, mainActivityContext, notifyCallback);
        state = ProviderState.IDLE;
        updateList();
    }

    public void updateList() {
        Thread thread = new Thread(() -> {
            itemList = getAppsFromServer();
            Log.i(TAG, "Loaded " + itemList.size() + " apps from YCCC server");
            mainActivityContext.ensureStoragePermissions();
            mainActivityContext.runOnUiThread(notifyCallback);
        });
        thread.start();
    }

    public void setHolder(int position, SideloadAdapter.ViewHolder holder) {
        SideloadItem item = itemList.get(position);
        holder.name.setText(item.getName());

        // All items are downloadable apps
        holder.size.setVisibility(View.VISIBLE);
        holder.downloadIcon.setVisibility(View.VISIBLE);
        holder.openFolderIcon.setVisibility(View.GONE);

        // Set click listener for download
        holder.layout.setOnClickListener(view -> {
            // Download will be handled by the adapter's download mechanism
            Log.i(TAG, "Selected app: " + item.getName());
        });
    }

    private ArrayList<SideloadItem> getAppsFromServer() {
        ArrayList<SideloadItem> items = new ArrayList<>();
        
        try {
            state = ProviderState.FETCHING;
            mainActivityContext.runOnUiThread(notifyCallback);

            // Fetch apps from our YCCC server
            URL u = new URL(SERVER_BASE_URL + "/apps");
            InputStream stream = u.openStream();
            int bufferSize = 1024;
            char[] buffer = new char[bufferSize];
            StringBuilder out = new StringBuilder();
            Reader in = new InputStreamReader(stream, StandardCharsets.UTF_8);
            for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
                out.append(buffer, 0, numRead);
            }
            
            JSONObject response = new JSONObject(out.toString());
            
            if (response.getBoolean("success")) {
                JSONArray appsArray = response.getJSONArray("apps");
                
                for (int i = 0; i < appsArray.length(); i++) {
                    JSONObject app = appsArray.getJSONObject(i);
                    
                    String name = app.getString("title");
                    String downloadUrl = SERVER_BASE_URL + "/download/" + app.getInt("id");
                    long size = app.optLong("fileSize", 0);
                    String developer = app.optString("developer", "Unknown");
                    String category = app.optString("category", "Apps");
                    
                    // Create display name with additional info
                    String displayName = name + " (" + category + " - " + developer + ")";
                    
                    items.add(new SideloadItem(
                        SideloadItemType.FILE, 
                        displayName, 
                        downloadUrl, 
                        size, 
                        "VR App"
                    ));
                }
            }

            state = ProviderState.IDLE;
            Log.i(TAG, "Successfully loaded " + items.size() + " apps from server");
            
        } catch (Exception e) {
            state = ProviderState.ERROR;
            Log.e(TAG, "Error fetching apps from server: " + e.toString());
            
            // Add fallback message
            items.add(new SideloadItem(
                SideloadItemType.FILE, 
                "Error: Could not connect to YCCC server", 
                "", 
                0, 
                "Check network connection"
            ));
        }
        
        mainActivityContext.runOnUiThread(notifyCallback);
        return items;
    }

    public void downloadFile(SideloadItem item, Consumer<File> startCallback, Consumer<Long> progressCallback, Consumer<File> completeCallback, Consumer<Exception> errorCallback) {
        File file = null;
        try {
            String fileUrl = item.getPath(); // This is already the full download URL
            final File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Files.createDirectories(Paths.get(dir.getAbsolutePath() + "/PicoZen"));
            
            // Extract clean filename from display name
            String fileName = item.getName().split(" \\(")[0] + ".apk";
            file = new File(dir.getAbsolutePath() + "/PicoZen/" + fileName);
            
            Log.i(TAG, "Downloading: " + fileName);
            int i = 1;
            while(file.exists()) {
                String baseName = fileName.substring(0, fileName.lastIndexOf("."));
                String extension = fileName.substring(fileName.lastIndexOf("."));
                file = new File(dir.getAbsolutePath() + "/PicoZen/" + baseName + " (" + i + ")" + extension);
                i++;
            }
            
            startCallback.accept(file);
            Log.i(TAG, "Download started for: " + file.getName());
            
            if(downloadFileFromUrl(fileUrl, file, progressCallback)) {
                completeCallback.accept(file);
                Log.i(TAG, "Download completed: " + file.getName());
            } else {
                file.delete();
                errorCallback.accept(new Exception("Download failed"));
            }
        } catch(Exception e) {
            Log.e(TAG, "Download error: " + e.toString());
            if(file != null && file.exists()) {
                file.delete();
            }
            errorCallback.accept(e);
        }
    }

    protected static boolean downloadFileFromUrl(String url, File outputFile, Consumer<Long> progressCallback) {
        try {
            return saveStream(new URL(url).openStream(), outputFile, progressCallback);
        } catch (Exception e) {
            Log.e("YCCCServerProvider", "Download error: " + e.toString());
            return false;
        }
    }

    protected static boolean saveStream(InputStream is, File outputFile, Consumer<Long> progressCallback) {
        try {
            DataInputStream dis = new DataInputStream(is);

            long processed = 0;
            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fos = new FileOutputStream(outputFile);

            while ((length = dis.read(buffer)) > 0) {
                if(!outputFile.canWrite()) {
                    fos.flush();
                    fos.close();
                    is.close();
                    dis.close();
                    return false;
                }

                fos.write(buffer, 0, length);
                fos.flush();
                processed += length;
                progressCallback.accept(processed);
            }
            fos.flush();
            fos.close();
            is.close();
            dis.close();

            return true;
        } catch (Exception e) {
            Log.e("YCCCServerProvider", "Save stream error: " + e.toString());
            return false;
        }
    }
}