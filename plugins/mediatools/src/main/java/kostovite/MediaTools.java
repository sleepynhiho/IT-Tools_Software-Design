package kostovite;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MediaTools {

    /**
     * Resize an image to the specified dimensions
     */
    public static Map<String, Object> resizeImage(byte[] imageData, int width, int height, String format) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Create resized image using imgscalr library for better quality
        BufferedImage resizedImage = Scalr.resize(originalImage, Scalr.Method.QUALITY, Scalr.Mode.FIT_EXACT, width, height);

        // Convert back to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, outputStream);
        byte[] resizedData = outputStream.toByteArray();

        // Return result
        result.put("data", resizedData);
        result.put("base64", Base64.getEncoder().encodeToString(resizedData));
        result.put("format", format);
        result.put("width", width);
        result.put("height", height);

        return result;
    }

    /**
     * Convert an image to a different format
     */
    public static Map<String, Object> convertImageFormat(byte[] imageData, String targetFormat) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Convert to target format
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, targetFormat, outputStream);
        byte[] convertedData = outputStream.toByteArray();

        // Return result
        result.put("data", convertedData);
        result.put("base64", Base64.getEncoder().encodeToString(convertedData));
        result.put("format", targetFormat);
        result.put("width", image.getWidth());
        result.put("height", image.getHeight());

        return result;
    }

    /**
     * Apply a filter to an image
     */
    public static Map<String, Object> applyFilter(byte[] imageData, String filter) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageData));
        if (originalImage == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Apply filter
        BufferedImage filteredImage;

        switch (filter.toLowerCase()) {
            case "grayscale":
                filteredImage = toGrayscale(originalImage);
                break;
            case "invert":
                filteredImage = invertColors(originalImage);
                break;
            case "sepia":
                filteredImage = toSepia(originalImage);
                break;
            default:
                result.put("error", "Unknown filter: " + filter);
                return result;
        }

        // Convert back to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(filteredImage, "png", outputStream);
        byte[] filteredData = outputStream.toByteArray();

        // Return result
        result.put("data", filteredData);
        result.put("base64", Base64.getEncoder().encodeToString(filteredData));
        result.put("filter", filter);
        result.put("width", filteredImage.getWidth());
        result.put("height", filteredImage.getHeight());

        return result;
    }

    /**
     * Get information about an image
     */
    public static Map<String, Object> getImageInfo(byte[] imageData) throws IOException {
        Map<String, Object> result = new HashMap<>();

        // Load the image
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            result.put("error", "Could not read input image");
            return result;
        }

        // Get image info
        result.put("width", image.getWidth());
        result.put("height", image.getHeight());
        result.put("type", getImageType(image.getType()));
        result.put("aspectRatio", (double) image.getWidth() / image.getHeight());
        result.put("pixelCount", image.getWidth() * image.getHeight());

        return result;
    }

    // Helper methods for filter effects

    private static BufferedImage toGrayscale(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage grayscaleImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        grayscaleImage.getGraphics().drawImage(original, 0, 0, null);
        return grayscaleImage;
    }

    private static BufferedImage invertColors(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage invertedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = 255 - ((rgb >> 16) & 0xff);
                int g = 255 - ((rgb >> 8) & 0xff);
                int b = 255 - (rgb & 0xff);
                int newRgb = (r << 16) | (g << 8) | b;
                invertedImage.setRGB(x, y, newRgb);
            }
        }

        return invertedImage;
    }

    private static BufferedImage toSepia(BufferedImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        BufferedImage sepiaImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = original.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;

                // Sepia formula
                int newRed = (int) (0.393 * r + 0.769 * g + 0.189 * b);
                int newGreen = (int) (0.349 * r + 0.686 * g + 0.168 * b);
                int newBlue = (int) (0.272 * r + 0.534 * g + 0.131 * b);

                // Clamp values
                newRed = Math.min(255, newRed);
                newGreen = Math.min(255, newGreen);
                newBlue = Math.min(255, newBlue);

                int newRgb = (newRed << 16) | (newGreen << 8) | newBlue;
                sepiaImage.setRGB(x, y, newRgb);
            }
        }

        return sepiaImage;
    }

    private static String getImageType(int type) {
        switch (type) {
            case BufferedImage.TYPE_3BYTE_BGR:
                return "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR:
                return "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_BINARY:
                return "BYTE_BINARY";
            case BufferedImage.TYPE_BYTE_GRAY:
                return "BYTE_GRAY";
            case BufferedImage.TYPE_BYTE_INDEXED:
                return "BYTE_INDEXED";
            case BufferedImage.TYPE_INT_ARGB:
                return "INT_ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return "INT_ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR:
                return "INT_BGR";
            case BufferedImage.TYPE_INT_RGB:
                return "INT_RGB";
            case BufferedImage.TYPE_USHORT_555_RGB:
                return "USHORT_555_RGB";
            case BufferedImage.TYPE_USHORT_565_RGB:
                return "USHORT_565_RGB";
            case BufferedImage.TYPE_USHORT_GRAY:
                return "USHORT_GRAY";
            default:
                return "CUSTOM";
        }
    }
}