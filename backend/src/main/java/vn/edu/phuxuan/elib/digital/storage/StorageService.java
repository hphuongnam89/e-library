package vn.edu.phuxuan.elib.digital.storage;

import java.io.InputStream;
import org.springframework.core.io.Resource;

public interface StorageService {

    /**
     * Stores a file stream securely. Validates MIME magic bytes for PDF and enforces size limits.
     * Computes SHA-256 integrity hash and returns storage key.
     */
    String store(InputStream inputStream, String originalFilename, String contentType, long sizeBytes);

    /**
     * Loads a specific byte range of a stored file.
     *
     * @param storageKey internal storage key
     * @param startByte start byte index (inclusive)
     * @param length number of bytes to read
     * @return InputStream containing the specified byte range
     */
    InputStream loadRange(String storageKey, long startByte, long length);

    /**
     * Loads the entire file as a Spring Resource.
     */
    Resource loadAsResource(String storageKey);

    /**
     * Returns file size in bytes.
     */
    long getFileSize(String storageKey);

    /**
     * Computes SHA-256 hex checksum of stored file.
     */
    String getChecksumSha256(String storageKey);

    /**
     * Deletes stored file.
     */
    void delete(String storageKey);
}
