package faang.school.projectservice.service.file;

import faang.school.projectservice.dto.file.ProcessedFile;
import faang.school.projectservice.exception.DataValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

@Service
public class ProjectCoverImageProcessor {
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_HORIZONTAL_WIDTH = 1080;
    private static final int MAX_HORIZONTAL_HEIGHT = 566;
    private static final int MAX_SQUARE_WIDTH = 1080;
    private static final int MAX_SQUARE_HEIGHT = 1080;

    public ProcessedFile process(MultipartFile file) {
        validateFile(file);

        try {
            byte[] originalContent = file.getBytes();
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(originalContent));
            if (originalImage == null) {
                throw new DataValidationException("Uploaded file must be an image");
            }

            ImageFormat format = detectImageFormat(file);
            ImageDimensions targetDimensions = calculateTargetDimensions(
                    originalImage.getWidth(),
                    originalImage.getHeight()
            );

            if (!targetDimensions.resized()) {
                return new ProcessedFile(originalContent, format.contentType(), format.extension());
            }

            byte[] resizedContent = resize(originalImage, targetDimensions, format);
            return new ProcessedFile(resizedContent, format.contentType(), format.extension());
        } catch (IOException e) {
            throw new DataValidationException("Failed to process uploaded cover image");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DataValidationException("Cover image must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new DataValidationException("Cover image size must not exceed 5 MB");
        }
    }

    private ImageFormat detectImageFormat(MultipartFile file) {
        String contentType = file.getContentType();
        if ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType)) {
            return ImageFormat.JPEG;
        }
        if ("image/png".equalsIgnoreCase(contentType)) {
            return ImageFormat.PNG;
        }

        String filename = file.getOriginalFilename();
        if (filename != null) {
            String lowerFilename = filename.toLowerCase(Locale.ROOT);
            if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                return ImageFormat.JPEG;
            }
            if (lowerFilename.endsWith(".png")) {
                return ImageFormat.PNG;
            }
        }

        throw new DataValidationException("Only JPEG and PNG cover images are supported");
    }

    private ImageDimensions calculateTargetDimensions(int width, int height) {
        int maxWidth = width > height ? MAX_HORIZONTAL_WIDTH : MAX_SQUARE_WIDTH;
        int maxHeight = width > height ? MAX_HORIZONTAL_HEIGHT : MAX_SQUARE_HEIGHT;

        if (width <= maxWidth && height <= maxHeight) {
            return new ImageDimensions(width, height, false);
        }

        double scale = Math.min((double) maxWidth / width, (double) maxHeight / height);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        return new ImageDimensions(targetWidth, targetHeight, true);
    }

    private byte[] resize(BufferedImage originalImage, ImageDimensions dimensions, ImageFormat format) throws IOException {
        int imageType = format == ImageFormat.PNG ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage resizedImage = new BufferedImage(dimensions.width(), dimensions.height(), imageType);
        Graphics2D graphics = resizedImage.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(originalImage, 0, 0, dimensions.width(), dimensions.height(), null);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format.imageIoFormat(), outputStream);
        return outputStream.toByteArray();
    }

    private record ImageDimensions(int width, int height, boolean resized) {
    }

    private enum ImageFormat {
        JPEG("image/jpeg", "jpg", "jpg"),
        PNG("image/png", "png", "png");

        private final String contentType;
        private final String extension;
        private final String imageIoFormat;

        ImageFormat(String contentType, String extension, String imageIoFormat) {
            this.contentType = contentType;
            this.extension = extension;
            this.imageIoFormat = imageIoFormat;
        }

        private String contentType() {
            return contentType;
        }

        private String extension() {
            return extension;
        }

        private String imageIoFormat() {
            return imageIoFormat;
        }
    }
}
