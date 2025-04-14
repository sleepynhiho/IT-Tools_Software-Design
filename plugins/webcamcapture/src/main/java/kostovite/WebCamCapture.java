package kostovite;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebCamCapture implements PluginInterface {

    private static final Logger logger = Logger.getLogger(WebCamCapture.class.getName());
    private final String uploadDir = "webcam-captures"; // Changed directory name
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public WebCamCapture() {
        // Create upload directory
        try {
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                if (directory.mkdirs()) {
                    logger.info("Created webcam capture directory: " + directory.getAbsolutePath());
                } else {
                    logger.severe("Failed to create webcam capture directory: " + directory.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error creating webcam capture directory", e);
        }
    }

    @Override
    public String getName() {
        return "WebCamCapture";
    }

    @Override
    public void execute() {
        logger.info("WebCamCapture Plugin executed (standalone test requires simulated data)");
    }

    @Override
    public Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", "WebCamCapture");
        metadata.put("name", "Webcam Capture");
        metadata.put("description", "Capture photos or record short videos using your device's camera.");
        metadata.put("icon", "CameraAlt");
        metadata.put("category", "Media");
        metadata.put("customUI", true);
        metadata.put("triggerUpdateOnChange", false);

        List<Map<String, Object>> sections = new ArrayList<>();

        Map<String, Object> controlSection = new HashMap<>();
        controlSection.put("id", "controls");
        controlSection.put("label", "Camera Feed & Capture");

        List<Map<String, Object>> inputs = new ArrayList<>();
        inputs.add(Map.ofEntries(
                Map.entry("id", "cameraFeed"),
                Map.entry("label", "Live Camera Preview"),
                Map.entry("type", "webcamPreview")
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "capturedImageData"),
                Map.entry("label", "Captured Image Data"),
                Map.entry("type", "hidden")
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "capturedVideoData"),
                Map.entry("label", "Captured Video Data"),
                Map.entry("type", "hidden")
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "capturedVideoMimeType"),
                Map.entry("label", "Captured Video MIME Type"),
                Map.entry("type", "hidden")
        ));
        inputs.add(Map.ofEntries(
                Map.entry("id", "outputFileName"),
                Map.entry("label", "Filename (Optional):"),
                Map.entry("type", "text"),
                Map.entry("placeholder", "my_capture"),
                Map.entry("required", false),
                Map.entry("helperText", "Leave blank for automatic name.")
        ));

        controlSection.put("inputs", inputs);
        sections.add(controlSection);

        Map<String, Object> actionsSection = new HashMap<>();
        actionsSection.put("id", "actions");
        actionsSection.put("label", "Actions");

        List<Map<String, Object>> actionInputs = new ArrayList<>();
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "takePhotoButton"),
                Map.entry("label", "Take Photo"),
                Map.entry("type", "button"),
                Map.entry("action", "capturePhoto")
        ));
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "startRecordButton"),
                Map.entry("label", "Start Recording"),
                Map.entry("type", "button"),
                Map.entry("action", "startRecording"),
                Map.entry("color", "secondary")
        ));
        actionInputs.add(Map.ofEntries(
                Map.entry("id", "stopRecordButton"),
                Map.entry("label", "Stop Recording & Save"),
                Map.entry("type", "button"),
                Map.entry("action", "stopRecording"),
                Map.entry("color", "error")
        ));

        actionsSection.put("inputs", actionInputs);
        sections.add(actionsSection);

        Map<String, Object> resultsSection = new HashMap<>();
        resultsSection.put("id", "results");
        resultsSection.put("label", "Capture Result");
        resultsSection.put("condition", "success === true && typeof savedFileName !== 'undefined'");

        List<Map<String, Object>> resultOutputs = new ArrayList<>();
        resultOutputs.add(createOutputField("statusMessage", "Status", "text", null));
        resultOutputs.add(createOutputField("savedFileName", "Saved Filename", "text", null));
        resultOutputs.add(createOutputField("savedFileType", "File Type", "text", null));
        resultOutputs.add(createOutputField("savedFileSize", "File Size", "text", null));
        resultOutputs.add(createOutputField("videoCodec", "Video Codec", "text", "savedFileType === 'video'"));
        resultOutputs.add(createOutputField("capturedImagePreview", "Captured Image", "image", "savedFileType === 'image' && typeof capturedImagePreview !== 'undefined'"));

        resultsSection.put("outputs", resultOutputs);
        sections.add(resultsSection);

        Map<String, Object> errorSection = new HashMap<>();
        errorSection.put("id", "errorDisplay");
        errorSection.put("label", "Error");
        errorSection.put("condition", "success === false");

        List<Map<String, Object>> errorOutputs = new ArrayList<>();
        errorOutputs.add(createOutputField("errorMessage", "Details", "text", null));
        errorSection.put("outputs", errorOutputs);
        sections.add(errorSection);

        metadata.put("sections", sections);
        return metadata;
    }

    private Map<String, Object> createOutputField(String id, String label, String type, String condition) {
        Map<String, Object> field = new HashMap<>();
        field.put("id", id);
        if (label != null && !label.isEmpty()) {
            field.put("label", label);
        }
        field.put("type", type);
        if (condition != null && !condition.isEmpty()) {
            field.put("condition", condition);
        }
        if (id.toLowerCase().contains("error")) {
            field.put("style", "error");
        }
        if ("text".equals(type) && (id.toLowerCase().contains("size") || id.toLowerCase().contains("name"))) {
            field.put("monospace", true);
        }
        if ("image".equals(type)){
            field.put("maxWidth", 300);
            field.put("maxHeight", 300);
        }
        return field;
    }

    private String detectCodec(String mimeType) {
        if (mimeType == null) return "Unknown";
        mimeType = mimeType.toLowerCase();
        if (mimeType.contains("h264") || mimeType.contains("avc")) {
            return "H.264/AVC";
        } else if (mimeType.contains("h265") || mimeType.contains("hevc")) {
            return "H.265/HEVC";
        } else if (mimeType.contains("vp9")) {
            return "VP9";
        } else if (mimeType.contains("vp8")) {
            return "VP8";
        } else if (mimeType.contains("av1")) {
            return "AV1";
        } else if (mimeType.startsWith("video/mp4")) {
            return "H.264/AVC (assumed for MP4)";
        } else if (mimeType.startsWith("video/webm")) {
            return "VP8/VP9 (assumed for WebM)";
        } else if (mimeType.startsWith("video/ogg")) {
            return "Theora (assumed for Ogg)";
        } else if (mimeType.startsWith("video/")) {
            return "Unknown video codec";
        } else {
            return "Not a video format";
        }
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input) {
        String errorOutputId = "errorMessage";
        String photoDataId = "capturedImageData";
        String videoDataId = "capturedVideoData";
        String videoMimeTypeId = "capturedVideoMimeType";
        String fileNameId = "outputFileName";

        logger.info("Processing webcam capture request");
        logger.info("Available input fields: " + input.keySet());

        // Log details of received fields
        logger.info("Captured Image Data Length: " +
                (input.containsKey(photoDataId) && input.get(photoDataId) != null
                        ? String.valueOf(input.get(photoDataId).toString().length())
                        : "null"));
        logger.info("Captured Video Data Length: " +
                (input.containsKey(videoDataId) && input.get(videoDataId) != null
                        ? String.valueOf(input.get(videoDataId).toString().length())
                        : "null"));
        logger.info("Captured Video MIME Type: " +
                (input.containsKey(videoMimeTypeId) && input.get(videoMimeTypeId) != null
                        ? input.get(videoMimeTypeId).toString()
                        : "null"));
        logger.info("Output File Name: " +
                (input.containsKey(fileNameId) && input.get(fileNameId) != null
                        ? input.get(fileNameId).toString()
                        : "null"));

        String operation;
        if (input.containsKey(photoDataId) && input.get(photoDataId) != null &&
                !String.valueOf(input.get(photoDataId)).isEmpty()) {
            operation = "savePhoto";
            logger.info("Operation: Save Photo");
        } else if (input.containsKey(videoDataId) && input.get(videoDataId) != null &&
                !String.valueOf(input.get(videoDataId)).isEmpty()) {
            operation = "saveVideo";
            logger.info("Operation: Save Video");
        } else {
            logger.warning("No image or video data received in the request");
            return Map.of("success", false, errorOutputId, "No image or video data received from capture.");
        }

        Map<String, Object> result;
        try {
            String requestedFileName = getStringParam(input, fileNameId, null);

            switch (operation) {
                case "savePhoto":
                    String imageDataBase64 = getStringParam(input, photoDataId, null);
                    logger.info("Processing photo with data length: " + imageDataBase64.length());
                    result = saveCaptureData(imageDataBase64, requestedFileName, "image/jpeg", ".jpg");
                    if (result.get("success") == Boolean.TRUE) {
                        result.put("capturedImagePreview", "data:image/jpeg;base64," + imageDataBase64);
                    }
                    break;

                case "saveVideo":
                    String videoDataBase64 = getStringParam(input, videoDataId, null);
                    String mimeType = getStringParam(input, videoMimeTypeId, "video/webm");
                    logger.info("Processing video with MIME type: " + mimeType + " and data length: " + videoDataBase64.length());
                    String extension = mimeTypeToExtension(mimeType);
                    String codec = detectCodec(mimeType);
                    result = saveCaptureData(videoDataBase64, requestedFileName, mimeType, extension);
                    result.put("videoCodec", codec);
                    break;

                default:
                    throw new IllegalArgumentException("Unknown capture operation.");
            }

            result.put("success", true);
            logger.info("Operation completed successfully: " + result);
            return result;

        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, "Invalid argument error", e);
            return Map.of("success", false, errorOutputId, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "IO error saving capture", e);
            return Map.of("success", false, errorOutputId, "Error saving captured file: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error processing capture", e);
            return Map.of("success", false, errorOutputId, "Unexpected error saving capture: " + e.getMessage());
        }
    }

    private Map<String, Object> saveCaptureData(String base64Data, String reqFileName, String mimeType, String extension)
            throws IOException, IllegalArgumentException {

        Map<String, Object> result = new HashMap<>();
        String fileType = mimeType.startsWith("image") ? "image" : (mimeType.startsWith("video") ? "video" : "file");

        if (base64Data == null || base64Data.trim().isEmpty()) {
            throw new IllegalArgumentException(fileType + " data cannot be empty.");
        }

        if (base64Data.startsWith("data:")) {
            base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
        }

        base64Data = base64Data.trim().replaceAll("\\s", "");

        byte[] decodedBytes = Base64.getDecoder().decode(base64Data);

        String baseName = (reqFileName != null && !reqFileName.isBlank())
                ? reqFileName.replaceAll("[^a-zA-Z0-9_.-]", "_")
                : fileType + "_" + LocalDateTime.now().format(formatter);
        String finalFileName = baseName.endsWith(extension) ? baseName : baseName + extension;

        String filePath = uploadDir + File.separator + finalFileName;
        try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
            outputStream.write(decodedBytes);
        }

        result.put("statusMessage", fileType + " saved successfully.");
        result.put("savedFileName", finalFileName);
        result.put("savedFileType", fileType);
        result.put("savedFileSize", String.format(Locale.US, "%,d bytes", decodedBytes.length));

        return result;
    }

    private String mimeTypeToExtension(String mimeType) {
        if (mimeType == null) return ".bin";
        return switch (mimeType.toLowerCase()) {
            case "video/mp4" -> ".mp4";
            case "video/ogg" -> ".ogv";
            case "video/webm" -> ".webm";
            case "video/quicktime" -> ".mov";
            case "video/x-matroska" -> ".mkv";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> ".bin";
        };
    }

    private String getStringParam(Map<String, Object> input, String key, String defaultValue) throws IllegalArgumentException {
        Object value = input.get(key);
        if (value == null) {
            if (defaultValue == null) {
                throw new IllegalArgumentException("Missing required parameter: " + key);
            }
            return defaultValue;
        }
        return value.toString();
    }
}