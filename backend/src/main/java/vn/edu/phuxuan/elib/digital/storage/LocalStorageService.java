package vn.edu.phuxuan.elib.digital.storage;

import jakarta.annotation.PostConstruct;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootPath;
    private final long maxFileSizeBytes;

    public LocalStorageService(
            @Value("${elib.storage.local-dir:./data/storage}") String localDir,
            @Value("${elib.storage.max-file-size-bytes:52428800}") long maxFileSizeBytes) {
        this.rootPath = Paths.get(localDir).toAbsolutePath().normalize();
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage directory: " + rootPath, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, String contentType, long sizeBytes) {
        if (sizeBytes > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Dung lượng tệp vượt quá giới hạn cho phép (tối đa 50MB)");
        }

        try {
            BufferedInputStream buffered = new BufferedInputStream(inputStream);
            buffered.mark(10);
            byte[] magic = new byte[5];
            int read = buffered.read(magic);
            buffered.reset();

            // Validate PDF magic bytes (%PDF-)
            if (read < 5 || magic[0] != '%' || magic[1] != 'P' || magic[2] != 'D' || magic[3] != 'F' || magic[4] != '-') {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Tệp không phải định dạng PDF hợp lệ (không khớp PDF magic bytes)");
            }

            String storageKey = UUID.randomUUID() + ".pdf";
            Path target = resolvePath(storageKey);

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                 DigestInputStream dis = new DigestInputStream(buffered, md)) {
                dis.transferTo(out);
            }

            return storageKey;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu trữ tệp số", e);
        }
    }

    @Override
    public InputStream loadRange(String storageKey, long startByte, long length) {
        Path target = resolvePath(storageKey);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp lưu trữ");
        }

        try {
            FileChannel channel = FileChannel.open(target, StandardOpenOption.READ);
            channel.position(startByte);
            InputStream stream = Channels.newInputStream(channel);
            return new BoundedInputStream(stream, length, channel);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc dải byte tệp số", e);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) {
        Path target = resolvePath(storageKey);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp lưu trữ");
        }
        return new FileSystemResource(target);
    }

    @Override
    public long getFileSize(String storageKey) {
        Path target = resolvePath(storageKey);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp lưu trữ");
        }
        try {
            return Files.size(target);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lấy kích thước tệp", e);
        }
    }

    @Override
    public String getChecksumSha256(String storageKey) {
        Path target = resolvePath(storageKey);
        if (!Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp lưu trữ");
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(target)) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    md.update(buffer, 0, n);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể tính checksum SHA-256", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = resolvePath(storageKey);
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private Path resolvePath(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..") || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Storage key không hợp lệ");
        }
        Path target = rootPath.resolve(storageKey).normalize();
        if (!target.startsWith(rootPath)) {
            throw new SecurityException("Phát hiện hành vi Path Traversal");
        }
        return target;
    }

    private static class BoundedInputStream extends InputStream {
        private final InputStream in;
        private final AutoCloseable closeable;
        private long remaining;

        BoundedInputStream(InputStream in, long limit, AutoCloseable closeable) {
            this.in = in;
            this.remaining = limit;
            this.closeable = closeable;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) return -1;
            int b = in.read();
            if (b != -1) remaining--;
            return b;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) return -1;
            int toRead = (int) Math.min(len, remaining);
            int bytesRead = in.read(b, off, toRead);
            if (bytesRead != -1) remaining -= bytesRead;
            return bytesRead;
        }

        @Override
        public void close() throws IOException {
            try {
                in.close();
            } finally {
                if (closeable != null) {
                    try {
                        closeable.close();
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }
}
