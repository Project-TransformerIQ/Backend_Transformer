package com.example.transformer.service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/transformers")
public class TransformerImageController {

  private final Path root;

  public TransformerImageController(@Value("${app.storage.root:uploads}") String rootDir) {
    this.root = Path.of(rootDir).toAbsolutePath().normalize();
  }

  @GetMapping(value = "/{id}/maintenance", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listMaintenance(@PathVariable Long id) throws IOException {
    return listUrlsForType(id, "maintenance");
  }

  @GetMapping(value = "/{id}/baseline", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listBaseline(@PathVariable Long id) throws IOException {
    return listUrlsForType(id, "baseline");
  }

  // Optional: one endpoint with a query param ?type=MAINTENANCE|BASELINE
  @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<String> listByType(@PathVariable Long id,
                                 @RequestParam(name = "type", required = false) String type) throws IOException {
    if (!StringUtils.hasText(type)) {
      // return both (baseline + maintenance) if no type provided
      List<String> out = new ArrayList<>();
      out.addAll(listUrlsForType(id, "baseline"));
      out.addAll(listUrlsForType(id, "maintenance"));
      return out;
    }
    return listUrlsForType(id, type.trim().toLowerCase());
  }

  private List<String> listUrlsForType(Long id, String type) throws IOException {
    if (!type.equals("baseline") && !type.equals("maintenance")) {
      throw new IllegalArgumentException("type must be baseline or maintenance");
    }
    Path dir = root.resolve("transformers").resolve(String.valueOf(id)).resolve(type);
    if (!Files.exists(dir)) return List.of();

    try (Stream<Path> stream = Files.list(dir)) {
      return stream
          .filter(Files::isRegularFile)
          .sorted(Comparator.comparing(Path::getFileName)) // adjust if you want latest first
          .map(p -> toPublicUrl(root.relativize(p)))
          .toList();
    }
  }

  // Map a relative path under uploads/ -> /files/**
  private String toPublicUrl(Path relative) {
    String urlPath = relative.toString().replace('\\', '/');
    // URL-encode each path segment
    StringBuilder sb = new StringBuilder("/files/");
    for (String seg : urlPath.split("/")) {
      if (!seg.isEmpty()) {
        sb.append(URLEncoder.encode(seg, StandardCharsets.UTF_8)).append("/");
      }
    }
    // remove trailing slash added above
    if (sb.charAt(sb.length() - 1) == '/') sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }
}
