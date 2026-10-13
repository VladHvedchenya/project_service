package school.faang.project_service.service.file;

import faang.school.projectservice.dto.file.ProcessedFile;
import faang.school.projectservice.exception.DataValidationException;
import faang.school.projectservice.service.file.ProjectCoverImageProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProjectCoverImageProcessorTest {
    private final ProjectCoverImageProcessor processor = new ProjectCoverImageProcessor();

    @Test
    public void testProcessThrowsWhenFileIsTooLarge() {
        byte[] content = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", content);

        assertThrows(DataValidationException.class, () -> processor.process(file));
    }

    @Test
    public void testProcessResizesHorizontalImage() throws IOException {
        MockMultipartFile file = createImage(2160, 1132);

        ProcessedFile processedFile = processor.process(file);
        BufferedImage resizedImage = ImageIO.read(new ByteArrayInputStream(processedFile.content()));

        assertEquals("image/png", processedFile.contentType());
        assertEquals("png", processedFile.extension());
        assertTrue(resizedImage.getWidth() <= 1080);
        assertTrue(resizedImage.getHeight() <= 566);
    }

    private MockMultipartFile createImage(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, width, height);
        graphics.dispose();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return new MockMultipartFile("file", "cover.png", "image/png", outputStream.toByteArray());
    }
}
