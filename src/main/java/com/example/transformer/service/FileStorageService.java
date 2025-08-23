package com.example.transformer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

@Service
public class FileStorageService {

  private final Path root;
  private static final DateTimeFormatter TS_FMT =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

  public FileStorageService(@Value("${app.storage.root:uploads}") String rootDir) throws IOException {
    this.root = Path.of(rootDir).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  /**
   * Save a transformer image into:
   * uploads/transformers/{transformerId}/{baseline|maintenance}/timestamp-filename.ext
   *
   * @return the RELATIVE path from the storage root (e.g.
   *         "transformers/1/baseline/20250824...-photo.jpg")
   */
  public String saveTransformerImage(Long transformerId, String imageType, MultipartFile file) throws IOException {
    Objects.requireNonNull(transformerId, "transformerId is required");
    Objects.requireNonNull(imageType, "imageType is required");
    Objects.requireNonNull(file, "file is required");

    String typeDir = normalizeType(imageType); // "baseline" or "maintenance"

    // sanitize original name
    String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "upload"));
    if (original.contains("..")) {
      throw new IllegalArgumentException("Invalid file name");
    }

    String timestamp = TS_FMT.format(LocalDateTime.now());
    String safeName = timestamp + "-" + original;

    // uploads/transformers/{id}/{typeDir}
    Path dir = root.resolve("transformers")
                   .resolve(String.valueOf(transformerId))
                   .resolve(typeDir)
                   .normalize();

    Files.createDirectories(dir);

    Path dest = dir.resolve(safeName).normalize();

    try (InputStream in = file.getInputStream()) {
      Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
    }

    // return relative path (portable slashes)
    Path relative = root.relativize(dest);
    return relative.toString().replace('\\', '/');
  }

  /**
   * Load a file by RELATIVE path returned from saveTransformerImage.
   */
  public FileSystemResource load(String relativePath) {
    if (relativePath == null || relativePath.isBlank()) {
      throw new IllegalArgumentException("relativePath is required");
    }
    Path target = root.resolve(relativePath).normalize();

    // prevent path escape
    if (!target.startsWith(root)) {
      throw new IllegalArgumentException("Illegal path");
    }
    return new FileSystemResource(target);
  }

  private String normalizeType(String imageType) {
    String t = imageType.trim().toLowerCase(Locale.ROOT);
    return switch (t) {
      case "baseline" -> "baseline";
      case "maintenance", "maintain", "maintainance" -> "maintenance"; // be forgiving
      case "baseline_images", "maintenance_images" -> t.contains("baseline") ? "baseline" : "maintenance";
      default -> {
        // Also accept enum-like input: BASELINE/MAINTENANCE
        if ("baseline".equalsIgnoreCase(imageType)) yield "baseline";
        if ("maintenance".equalsIgnoreCase(imageType)) yield "maintenance";
        throw new IllegalArgumentException("imageType must be BASELINE or MAINTENANCE");
      }
    };
  }
}
