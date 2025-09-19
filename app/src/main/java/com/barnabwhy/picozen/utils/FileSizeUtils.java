package com.barnabwhy.picozen.utils;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class for handling file size calculations and formatting
 * Fixes the file size calculation issues in PicoZen downloads
 */
public class FileSizeUtils {
    
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB", "TB"};
    
    /**
     * Format bytes to human readable string
     * @param bytes File size in bytes
     * @return Formatted string (e.g., "150.5 MB")
     */
    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < SIZE_UNITS.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return DECIMAL_FORMAT.format(size) + " " + SIZE_UNITS[unitIndex];
    }
    
    /**
     * Parse file size string to bytes
     * @param sizeString String like "150.5 MB" or "1.2 GB"
     * @return Size in bytes, or -1 if parsing fails
     */
    public static long parseFileSize(String sizeString) {
        if (sizeString == null || sizeString.trim().isEmpty()) {
            return -1;
        }
        
        try {
            sizeString = sizeString.trim().toLowerCase();
            
            // Extract number and unit
            String[] parts = sizeString.split("\\s+");
            if (parts.length != 2) {
                return -1;
            }
            
            double size = Double.parseDouble(parts[0]);
            String unit = parts[1];
            
            // Convert to bytes based on unit
            switch (unit) {
                case "b":
                case "byte":
                case "bytes":
                    return (long) size;
                case "kb":
                case "kilobyte":
                case "kilobytes":
                    return (long) (size * 1024);
                case "mb":
                case "megabyte":
                case "megabytes":
                    return (long) (size * 1024 * 1024);
                case "gb":
                case "gigabyte":
                case "gigabytes":
                    return (long) (size * 1024 * 1024 * 1024);
                case "tb":
                case "terabyte":
                case "terabytes":
                    return (long) (size * 1024 * 1024 * 1024 * 1024);
                default:
                    return -1;
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    /**
     * Get file size from local file
     * @param file Local file
     * @return Size in bytes, or -1 if file doesn't exist
     */
    public static long getLocalFileSize(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return -1;
        }
        return file.length();
    }
    
    /**
     * Get file size from remote URL using HEAD request
     * @param url Remote file URL
     * @param client OkHttp client for network requests
     * @return Size in bytes, or -1 if unable to determine
     */
    public static long getRemoteFileSize(String url, OkHttpClient client) {
        if (url == null || url.trim().isEmpty()) {
            return -1;
        }
        
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .head() // Use HEAD request to get headers only
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String contentLength = response.header("Content-Length");
                    if (contentLength != null) {
                        return Long.parseLong(contentLength);
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Ignore and return -1
        }
        
        return -1;
    }
    
    /**
     * Calculate download progress percentage
     * @param downloadedBytes Bytes downloaded so far
     * @param totalBytes Total file size in bytes
     * @return Progress percentage (0-100), or -1 if invalid
     */
    public static int calculateProgress(long downloadedBytes, long totalBytes) {
        if (totalBytes <= 0 || downloadedBytes < 0) {
            return -1;
        }
        
        if (downloadedBytes >= totalBytes) {
            return 100;
        }
        
        // Calculate percentage and ensure it doesn't exceed 100
        double progress = ((double) downloadedBytes / totalBytes) * 100.0;
        return Math.min(100, Math.max(0, (int) Math.round(progress)));
    }
    
    /**
     * Format download progress string
     * @param downloadedBytes Bytes downloaded
     * @param totalBytes Total file size
     * @return Formatted string like "150.5 MB / 200.0 MB (75%)"
     */
    public static String formatDownloadProgress(long downloadedBytes, long totalBytes) {
        if (totalBytes <= 0) {
            return formatFileSize(downloadedBytes) + " / Unknown (--%)";
        }
        
        int progress = calculateProgress(downloadedBytes, totalBytes);
        return String.format(Locale.US, "%s / %s (%d%%)", 
                formatFileSize(downloadedBytes), 
                formatFileSize(totalBytes), 
                progress);
    }
    
    /**
     * Validate that download progress makes sense
     * @param downloadedBytes Bytes downloaded
     * @param totalBytes Total file size
     * @return true if progress is valid, false if there's an error
     */
    public static boolean isValidProgress(long downloadedBytes, long totalBytes) {
        return totalBytes > 0 && 
               downloadedBytes >= 0 && 
               downloadedBytes <= totalBytes;
    }
    
    /**
     * Fix invalid file size data by attempting to detect the correct size
     * @param reportedSize The size reported by the server/source
     * @param actualDownloaded The actual bytes downloaded
     * @return Corrected total size, or the original if unable to fix
     */
    public static long fixInvalidFileSize(long reportedSize, long actualDownloaded) {
        // If downloaded amount exceeds reported size significantly, 
        // the reported size is likely wrong
        if (reportedSize > 0 && actualDownloaded > reportedSize * 1.1) {
            // Estimate the real size based on download progress
            // This is a heuristic - in practice you'd want to get the real size from headers
            return Math.max(reportedSize, actualDownloaded);
        }
        
        return reportedSize;
    }
}