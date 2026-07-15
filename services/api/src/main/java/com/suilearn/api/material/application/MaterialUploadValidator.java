package com.suilearn.api.material.application;

import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.MaterialSourceType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Validates an upload into a temporary file without materializing document bytes in heap memory. */
@Component
public class MaterialUploadValidator {
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};
    private static final byte[] OLE = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1};
    private final long maxBytes;
    private final int maxPdfPages;

    @Autowired
    public MaterialUploadValidator(com.suilearn.api.config.SuiLearnProcessingProperties properties) {
        this((long) properties.maxFileSizeMb() * 1024 * 1024, properties.pdfMaxPages());
    }

    MaterialUploadValidator(long maxBytes, int maxPdfPages) {
        this.maxBytes = maxBytes;
        this.maxPdfPages = maxPdfPages;
    }

    public ValidatedUpload validate(MaterialSourceType sourceType, AssetUpload upload) {
        validateMetadata(sourceType, upload.originalFilename(), upload.mimeType());
        Path file = null;
        try {
            file = Files.createTempFile("suilearn-upload-", ".bin");
            byte[] prefix = new byte[8];
            int prefixLength = 0;
            long size = 0;
            try (InputStream input = upload.stream(); var output = Files.newOutputStream(file)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) != -1;) {
                    size += read;
                    if (size > maxBytes) {
                        throw error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the configured size limit");
                    }
                    int copied = Math.min(read, prefix.length - prefixLength);
                    if (copied > 0) {
                        System.arraycopy(buffer, 0, prefix, prefixLength, copied);
                        prefixLength += copied;
                    }
                    output.write(buffer, 0, read);
                }
            }
            if (!hasExpectedSignature(sourceType, prefix, prefixLength)) {
                throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Uploaded file signature does not match its declared type");
            }
            if (sourceType == MaterialSourceType.PDF) {
                validatePdfPageCount(file);
            }
            return new ValidatedUpload(file, upload.originalFilename(), upload.mimeType());
        } catch (IOException exception) {
            delete(file);
            throw error(HttpStatus.BAD_REQUEST, "Uploaded file could not be read safely");
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException) {
                delete(file);
                throw exception;
            }
            delete(file);
            throw error(HttpStatus.BAD_REQUEST, "Uploaded file is damaged or invalid");
        }
    }

    private void validateMetadata(MaterialSourceType sourceType, String filename, String mimeType) {
        String extension = filename == null ? "" : filename.substring(Math.max(filename.lastIndexOf('.'), 0) + 1).toLowerCase(Locale.ROOT);
        String declaredMime = mimeType == null ? "" : mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean accepted = switch (sourceType) {
            case MARKDOWN -> (extension.equals("md") || extension.equals("markdown")) && declaredMime.equals("text/markdown");
            case TXT -> extension.equals("txt") && declaredMime.equals("text/plain");
            case PDF -> extension.equals("pdf") && declaredMime.equals("application/pdf");
            case DOC -> extension.equals("doc") && declaredMime.equals("application/msword");
            case DOCX -> extension.equals("docx") && declaredMime.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        };
        if (!accepted) {
            throw error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "File extension and MIME type must match the selected source type");
        }
    }

    private boolean hasExpectedSignature(MaterialSourceType sourceType, byte[] prefix, int length) {
        return switch (sourceType) {
            case MARKDOWN, TXT -> true;
            case PDF -> startsWith(prefix, length, PDF);
            case DOC -> startsWith(prefix, length, OLE);
            case DOCX -> length >= 4 && prefix[0] == 'P' && prefix[1] == 'K' && prefix[2] == 3 && prefix[3] == 4;
        };
    }

    private void validatePdfPageCount(Path file) {
        try (var document = Loader.loadPDF(file.toFile())) {
            if (document.getNumberOfPages() > maxPdfPages) {
                throw error(HttpStatus.PAYLOAD_TOO_LARGE, "PDF exceeds the configured page limit");
            }
        } catch (IOException exception) {
            throw error(HttpStatus.BAD_REQUEST, "PDF is damaged or unreadable");
        }
    }

    private static boolean startsWith(byte[] value, int length, byte[] expected) {
        return length >= expected.length && Arrays.equals(Arrays.copyOf(value, expected.length), expected);
    }

    private static ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    private static void delete(Path file) {
        if (file != null) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) { }
        }
    }

    public static final class ValidatedUpload implements AutoCloseable {
        private final Path file;
        private final String filename;
        private final String mimeType;

        private ValidatedUpload(Path file, String filename, String mimeType) {
            this.file = file;
            this.filename = filename;
            this.mimeType = mimeType;
        }

        public AssetUpload openAssetUpload() {
            try {
                return new AssetUpload(Files.newInputStream(file), filename, mimeType);
            } catch (IOException exception) {
                throw error(HttpStatus.BAD_REQUEST, "Validated upload is no longer readable");
            }
        }

        @Override public void close() { delete(file); }
    }
}
