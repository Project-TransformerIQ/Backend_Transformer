package com.example.transformer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
public class FileStorageService {
  private final Path root;

  public FileStorageService(@Value("${app.storage.root:uploads}") String rootDir) throws IOException {
    this.root = Path.of(rootDir).toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  public String saveTransformerImage(Long transformerId, MultipartFile file) throws IOException {
    String safeName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
    String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").format(LocalDateTime.now());
    Path dir = root.resolve("transformers").resolve(String.valueOf(transformerId));
    Files.createDirectories(dir);
    Path path = dir.resolve(ts + "-" + safeName);
    try (InputStream in = file.getInputStream()) {
      Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
    }
    return path.toString();
  }

  public FileSystemResource load(String absPath) {
    return new FileSystemResource(absPath);
  }
}
