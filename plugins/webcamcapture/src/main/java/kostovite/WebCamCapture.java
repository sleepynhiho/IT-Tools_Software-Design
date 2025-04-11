package kostovite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class WebCamCapture implements PluginInterface {

    private final String uploadDir = "media-uploads";
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public WebCamCapture() {
        // Create upload directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public String getName() {
        return "WebCamCapture";
    }

    @Override
    public void execute() {
        System.out.println("WebCamCapture Plugin executed");
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("name", getName());
        metadata.put("version", "1.0.0");
        metadata.put("description", "Capture images and videos from webcam");

        // Define available operations
        Map<String, Object> operations = new HashMap<>();

        // Take Photo operation
        Map<String, Object> takePhotoOperation = new HashMap<>();
        takePhotoOperation.put("description", "Take a photo from webcam");

        Map<String, Object> photoInputs = new HashMap<>();
        photoInputs.put("imageData", "Base64 encoded image data");
        photoInputs.put("fileName", "Optional file name (default: auto-generated)");

        takePhotoOperation.put("inputs", photoInputs);
        operations.put("takePhoto", takePhotoOperation);

        // Record Video operation
        Map<String, Object> recordVideoOperation = new HashMap<>();
        recordVideoOperation.put("description", "Save recorded video from webcam");

        Map<String, Object> videoInputs = new HashMap<>();
        videoInputs.put("videoData", "Base64 encoded video data");
        videoInputs.put("fileName", "Optional file name (default: auto-generated)");
        videoInputs.put("mimeType", "MIME type of the video");

        recordVideoOperation.put("inputs", videoInputs);
        operations.put("recordVideo", recordVideoOperation);

        // Get Client Script operation
        Map<String, Object> getScriptOperation = new HashMap<>();
        getScriptOperation.put("description", "Get client-side JavaScript for webcam capture");
        operations.put("getClientScript", getScriptOperation);

        metadata.put("operations", operations);
        return metadata;
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        Map<String, Object> result = new HashMap<>();

        try {
            String operation = (String) input.getOrDefault("operation", "");

            return switch (operation.toLowerCase()) {
                case "takephoto" -> processImage(input);
                case "recordvideo" -> processVideo(input);
                case "getclientscript" -> getClientScript();
                default -> {
                    result.put("error", "Unsupported operation: " + operation);
                    yield result;
                }
            };
        } catch (Exception e) {
            result.put("error", "Error processing request: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private Map<String, Object> processImage(Map<String, Object> input) throws IOException {
        Map<String, Object> result = new HashMap<>();

        String imageData = (String) input.get("imageData");
        if (imageData == null || imageData.isEmpty()) {
            result.put("error", "No image data provided");
            return result;
        }

        // Remove data URL prefix if present (e.g., "data:image/png;base64,")
        if (imageData.contains(",")) {
            imageData = imageData.substring(imageData.indexOf(",") + 1);
        }

        // Decode base64 data
        byte[] decodedBytes = Base64.getDecoder().decode(imageData);

        // Generate file name if not provided
        String fileName = (String) input.getOrDefault("fileName", "photo_" + LocalDateTime.now().format(formatter) + ".jpg");

        // Ensure the file has the correct extension
        if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg") && !fileName.endsWith(".png")) {
            fileName = fileName + ".jpg";
        }

        // Save image file
        String filePath = uploadDir + File.separator + fileName;
        FileOutputStream outputStream = new FileOutputStream(filePath);
        outputStream.write(decodedBytes);
        outputStream.close();

        // Return success response
        result.put("success", true);
        result.put("fileName", fileName);
        result.put("filePath", filePath);
        result.put("fileSize", decodedBytes.length);
        result.put("fileType", "image");

        return result;
    }

    private Map<String, Object> processVideo(Map<String, Object> input) throws IOException {
        Map<String, Object> result = new HashMap<>();

        String videoData = (String) input.get("videoData");
        if (videoData == null || videoData.isEmpty()) {
            result.put("error", "No video data provided");
            return result;
        }

        // Remove data URL prefix if present
        if (videoData.contains(",")) {
            videoData = videoData.substring(videoData.indexOf(",") + 1);
        }

        // Decode base64 data
        byte[] decodedBytes = Base64.getDecoder().decode(videoData);

        // Get MIME type to determine file extension
        String mimeType = (String) input.getOrDefault("mimeType", "video/webm");
        String extension = mimeTypeToExtension(mimeType);

        // Generate file name if not provided
        String fileName = (String) input.getOrDefault("fileName", "video_" + LocalDateTime.now().format(formatter) + extension);

        // Ensure the file has the correct extension
        if (!fileName.endsWith(extension)) {
            fileName = fileName + extension;
        }

        // Save video file
        String filePath = uploadDir + File.separator + fileName;
        FileOutputStream outputStream = new FileOutputStream(filePath);
        outputStream.write(decodedBytes);
        outputStream.close();

        // Return success response
        result.put("success", true);
        result.put("fileName", fileName);
        result.put("filePath", filePath);
        result.put("fileSize", decodedBytes.length);
        result.put("fileType", "video");
        result.put("mimeType", mimeType);

        return result;
    }

    private Map<String, Object> getClientScript() {
        Map<String, Object> result = new HashMap<>();

        try {
            // Read the client-side script
            Path scriptPath = Paths.get(Objects.requireNonNull(getClass().getResource("/static/webcam.js")).toURI());
            String script = new String(Files.readAllBytes(scriptPath));

            result.put("success", true);
            result.put("script", script);

        } catch (Exception e) {
            result.put("error", "Failed to load client script: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    private String mimeTypeToExtension(String mimeType) {
        return switch (mimeType.toLowerCase()) {
            case "video/mp4" -> ".mp4";
            case "video/ogg" -> ".ogv";
            default -> ".webm";
        };
    }
}