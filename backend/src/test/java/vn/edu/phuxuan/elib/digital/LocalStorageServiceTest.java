package vn.edu.phuxuan.elib.digital;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import vn.edu.phuxuan.elib.digital.storage.LocalStorageService;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalStorageService(tempDir.toString(), 52428800L);
        storageService.init();
    }

    private byte[] createMockPdfBytes(int size) {
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
        byte[] trailer = "\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
        int paddingSize = Math.max(0, size - header.length - trailer.length);
        byte[] padding = new byte[paddingSize];
        Arrays.fill(padding, (byte) 'B');

        byte[] result = new byte[header.length + padding.length + trailer.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(padding, 0, result, header.length, padding.length);
        System.arraycopy(trailer, 0, result, header.length + padding.length, trailer.length);
        return result;
    }

    @Test
    void storesValidPdfAndComputesChecksum() throws Exception {
        byte[] pdfBytes = createMockPdfBytes(500);
        String key = storageService.store(new ByteArrayInputStream(pdfBytes), "sample.pdf", "application/pdf", pdfBytes.length);

        assertThat(key).isNotNull().endsWith(".pdf");
        assertThat(storageService.getFileSize(key)).isEqualTo(pdfBytes.length);

        String checksum = storageService.getChecksumSha256(key);
        assertThat(checksum).isNotNull().hasSize(64); // hex SHA-256

        try (InputStream stream = storageService.loadRange(key, 0, pdfBytes.length)) {
            byte[] loaded = stream.readAllBytes();
            assertThat(loaded).isEqualTo(pdfBytes);
        }
    }

    @Test
    void rejectsInvalidMagicBytes() {
        byte[] invalidBytes = "THIS IS NOT A PDF".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storageService.store(
                new ByteArrayInputStream(invalidBytes), "fake.pdf", "application/pdf", invalidBytes.length))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tệp không phải định dạng PDF hợp lệ");
    }

    @Test
    void rejectsExceededFileSize() {
        byte[] pdfBytes = createMockPdfBytes(100);

        assertThatThrownBy(() -> storageService.store(
                new ByteArrayInputStream(pdfBytes), "huge.pdf", "application/pdf", 60000000L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("vượt quá giới hạn cho phép");
    }

    @Test
    void loadsByteRangeCorrectly() throws Exception {
        byte[] pdfBytes = createMockPdfBytes(1024);
        String key = storageService.store(new ByteArrayInputStream(pdfBytes), "range.pdf", "application/pdf", pdfBytes.length);

        // Load bytes 100 to 299 (length 200)
        try (InputStream stream = storageService.loadRange(key, 100, 200)) {
            byte[] rangeBytes = stream.readAllBytes();
            assertThat(rangeBytes.length).isEqualTo(200);
            assertThat(rangeBytes).isEqualTo(Arrays.copyOfRange(pdfBytes, 100, 300));
        }
    }

    @Test
    void preventsPathTraversalAttempts() {
        assertThatThrownBy(() -> storageService.loadRange("../secret.txt", 0, 10))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Storage key không hợp lệ");

        assertThatThrownBy(() -> storageService.getFileSize("/etc/passwd"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Storage key không hợp lệ");
    }

    @Test
    void deletesStoredFile() {
        byte[] pdfBytes = createMockPdfBytes(200);
        String key = storageService.store(new ByteArrayInputStream(pdfBytes), "delete.pdf", "application/pdf", pdfBytes.length);

        assertThat(Files.exists(tempDir.resolve(key))).isTrue();
        storageService.delete(key);
        assertThat(Files.exists(tempDir.resolve(key))).isFalse();
    }
}
