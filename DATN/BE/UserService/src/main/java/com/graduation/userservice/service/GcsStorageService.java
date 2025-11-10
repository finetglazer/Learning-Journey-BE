package com.graduation.userservice.service;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.graduation.userservice.constant.Constant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GcsStorageService {

    @Autowired
    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    @Value("${gcs.base-url}")
    private String baseUrl;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png"
    );
    
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int TARGET_WIDTH = 500;
    private static final int TARGET_HEIGHT = 500;

    /**
     * Upload and resize avatar image to GCS
     * @param userId User ID
     * @param file Multipart file
     * @return Public URL of uploaded file
     */
    public String uploadAvatar(Long userId, MultipartFile file) throws IOException {
        // Validate file
        validateFile(file);

        // Determine file extension
        String contentType = file.getContentType();
        String extension = getExtensionFromContentType(contentType);
        
        // Resize image
        byte[] resizedImageBytes = resizeImage(file.getBytes(), extension);

        // Define GCS path: users/{userId}/avatar.{extension}
        String objectName = String.format("users/%d/avatar.%s", userId, extension);
        BlobId blobId = BlobId.of(bucketName, objectName);

        // Create blob info with public read ACL
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                // .setAcl(...) line is completely removed
                .build();

        // Upload to GCS
        storage.create(blobInfo, resizedImageBytes);

        // Construct and return public URL
        String publicUrl = String.format("%s/%s/%s", baseUrl, bucketName, objectName);
        log.info(Constant.LOG_AVATAR_UPLOADED_SUCCESS, userId, publicUrl);
        
        return publicUrl;
    }

    /**
     * Delete avatar from GCS
     * @param userId User ID
     */
    public void deleteAvatar(Long userId) {
        try {
            // Try all possible extensions
            for (String ext : Arrays.asList("jpg", "jpeg", "png")) {
                String objectName = String.format("users/%d/avatar.%s", userId, ext);
                BlobId blobId = BlobId.of(bucketName, objectName);
                boolean deleted = storage.delete(blobId);
                if (deleted) {
                    log.info(Constant.LOG_AVATAR_DELETED_SUCCESS, userId, objectName);
                    return;
                }
            }
            log.warn(Constant.LOG_AVATAR_NOT_FOUND, userId);
        } catch (Exception e) {
            log.error(Constant.LOG_AVATAR_DELETE_FAILED, userId, e);
        }
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(Constant.MSG_FILE_REQUIRED);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            // This exception is part of Spring and is more descriptive
            throw new MaxUploadSizeExceededException(MAX_FILE_SIZE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(Constant.MSG_INVALID_FILE_TYPE);
        }
    }

    /**
     * Get file extension from content type
     */
    private String getExtensionFromContentType(String contentType) {
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return "jpg";
        } else if (contentType.contains("png")) {
            return "png";
        }
        return "jpg"; // default
    }

    /**
     * Resize image to target dimensions
     */
    private byte[] resizeImage(byte[] originalImageBytes, String format) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(originalImageBytes);
        BufferedImage originalImage = ImageIO.read(bais);

        if (originalImage == null) {
            throw new IOException(Constant.MSG_INVALID_IMAGE_DATA);
        }

        // Calculate dimensions maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        
        int newWidth = TARGET_WIDTH;
        int newHeight = TARGET_HEIGHT;

        // Maintain aspect ratio
        double aspectRatio = (double) originalWidth / originalHeight;
        if (aspectRatio > 1) {
            newHeight = (int) (newWidth / aspectRatio);
        } else {
            newWidth = (int) (newHeight * aspectRatio);
        }

        // Create resized image
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        // Convert to byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resizedImage, format, baos);
        
        return baos.toByteArray();
    }
}
