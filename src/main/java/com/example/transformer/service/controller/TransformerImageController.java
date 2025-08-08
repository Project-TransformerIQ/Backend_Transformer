package com.example.transformer.service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;  
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import static java.util.Comparator.comparing;


@RestController
@RequestMapping("/api/transformers")
public class TransformerImageController {

  private final Path root;

  public TransformerImageController(@Value("${app.storage.root:uploads}") String rootDir) {
    this.root = Path.of(rootDir).toAbsolutePath().normalize();
  }

  

  @GetMapping(value = "/{id}/maintenance", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listMaintenance(@PathVariable Long id) throws IOException {
    return listUrlsForType(id, "maintenance", null);
  }

  @GetMapping(value = "/{id}/baseline", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listBaseline(@PathVariable Long id) throws IOException {
    return listUrlsForType(id, "baseline", null);
  }

  
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listByType(@PathVariable Long id,
                                 @RequestParam(name = "type", required = false) String type,
                                 @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
    if (!StringUtils.hasText(type)) {
      List<String> out = new ArrayList<>();
      out.addAll(listUrlsForType(id, "baseline", limit));
      out.addAll(listUrlsForType(id, "maintenance", limit));
      return out;
    }
    return listUrlsForType(id, type.trim().toLowerCase(), limit);
  }

  // --------------------------------------
  // NEW: Base64 (data URI) list endpoints
  // --------------------------------------

  @GetMapping(value = "/{id}/baseline/base64", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ImageDto> listBaselineBase64(@PathVariable Long id,
                                           @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
    return listImagesForTypeBase64(id, "baseline", limit);
  }

  @GetMapping(value = "/{id}/maintenance/base64", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<ImageDto> listMaintenanceBase64(@PathVariable Long id,
                                              @RequestParam(name = "limit", required = false) Integer limit) throws IOException {
    return listImagesForTypeBase64(id, "maintenance", limit);
  }

  // ----------------------------
  // Helpers
  // ----------------------------

  private List<String> listUrlsForType(Long id, String type, Integer limit) throws IOException {
    Path dir = validateTypeAndResolveDir(id, type);
    if (!Files.exists(dir)) return List.of();

    try (Stream<Path> stream = Files.list(dir)) {
      List<Path> files = stream
          .filter(Files::isRegularFile)
          .sorted(byLastModifiedDescending())
          .collect(Collectors.toList());

      if (limit != null && limit > 0 && limit < files.size()) {
        files = files.subList(0, limit);
      }

      return files.stream()
          .map(p -> toPublicUrl(root.relativize(p)))
          .collect(Collectors.toList());
    }
  }

  private List<ImageDto> listImagesForTypeBase64(Long id, String type, Integer limit) throws IOException {
    Path dir = validateTypeAndResolveDir(id, type);
    if (!Files.exists(dir)) return List.of();

    try (Stream<Path> stream = Files.list(dir)) {
      List<Path> files = stream
          .filter(Files::isRegularFile)
          .sorted(byLastModifiedDescending())
          .collect(Collectors.toList());

      if (limit != null && limit > 0 && limit < files.size()) {
        files = files.subList(0, limit);
      }

      List<ImageDto> out = new ArrayList<>(files.size());
      for (Path p : files) {
        out.add(toImageDto(p));
      }
      return out;
    }
  }

  private Comparator<Path> byLastModifiedDescending() {
    return (a, b) -> {
      try {
        FileTime ta = Files.readAttributes(a, BasicFileAttributes.class).lastModifiedTime();
        FileTime tb = Files.readAttributes(b, BasicFileAttributes.class).lastModifiedTime();
        return tb.compareTo(ta); // newest first
      } catch (IOException e) {
        // fallback to filename compare if attrs fail
        return comparing((Path x) -> x.getFileName().toString()).reversed().compare(a, b);
      }
    };
  }

  private Path validateTypeAndResolveDir(Long id, String type) {
    String normalized = type == null ? "" : type.toLowerCase(Locale.ROOT);
    if (!normalized.equals("baseline") && !normalized.equals("maintenance")) {
      throw new IllegalArgumentException("type must be baseline or maintenance");
    }
    return root.resolve("transformers").resolve(String.valueOf(id)).resolve(normalized);
  }

  private ImageDto toImageDto(Path file) throws IOException {
    String filename = file.getFileName().toString();
    String contentType = Files.probeContentType(file);
    if (contentType == null) {
      contentType = guessContentTypeByExtension(filename);
    }
    long size = Files.size(file);
    Instant lastModified = Files.getLastModifiedTime(file).toInstant();

    byte[] bytes = Files.readAllBytes(file);
    String base64 = Base64.getEncoder().encodeToString(bytes);
    String dataUri = "data:" + contentType + ";base64," + base64;

    
    String publicUrl = toPublicUrl(root.relativize(file));

    return new ImageDto(filename, contentType, size, lastModified.toString(), dataUri, publicUrl);
  }

  private String guessContentTypeByExtension(String filename) {
    String lower = filename.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".png")) return "image/png";
    if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
    if (lower.endsWith(".gif")) return "image/gif";
    if (lower.endsWith(".bmp")) return "image/bmp";
    if (lower.endsWith(".webp")) return "image/webp";
    return "application/octet-stream";
  }

  // Map a relative path under uploads/ -> /files/**
  private String toPublicUrl(Path relative) {
    String urlPath = relative.toString().replace('\\', '/');
    StringBuilder sb = new StringBuilder("/files/");
    for (String seg : urlPath.split("/")) {
      if (!seg.isEmpty()) {
        sb.append(URLEncoder.encode(seg, StandardCharsets.UTF_8)).append("/");
      }
    }
    if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '/') {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  // --------------------------------------
  // DTO
  // --------------------------------------

  /**
   * Base64 image payload for the UI:
   * - data: data URI (data:<mime>;base64,<...>)
   * - publicUrl: optional (still useful if you want to link/download)
   */
  public static class ImageDto {
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String lastModified; // ISO-8601
    private String data;         // data URI
    private String publicUrl;    // optional convenience

    public ImageDto() {}

    public ImageDto(String filename, String contentType, long sizeBytes, String lastModified, String data, String publicUrl) {
      this.filename = filename;
      this.contentType = contentType;
      this.sizeBytes = sizeBytes;
      this.lastModified = lastModified;
      this.data = data;
      this.publicUrl = publicUrl;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getLastModified() { return lastModified; }
    public void setLastModified(String lastModified) { this.lastModified = lastModified; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
  }
}
