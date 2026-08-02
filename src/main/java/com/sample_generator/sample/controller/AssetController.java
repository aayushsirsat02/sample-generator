package com.sample_generator.sample.controller;

import com.sample_generator.sample.Entity.Asset;
import com.sample_generator.sample.repository.AssetRepository;
import com.sample_generator.sample.service.AssetReferenceService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetRepository assetRepository;
    private final AssetReferenceService assetReferenceService;
    private final Path fileStorageLocation;

    public AssetController(AssetRepository assetRepository, AssetReferenceService assetReferenceService) {
        this.assetRepository = assetRepository;
        this.assetReferenceService = assetReferenceService;
        this.fileStorageLocation = Paths.get("uploads/assets").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllAssets(@RequestParam(required = false) String category) {
        List<Asset> assets = (category != null && !category.isBlank())
                ? assetRepository.findByCategory(category)
                : assetRepository.findAll();
        return ResponseEntity.ok(assets.stream().map(this::toAssetView).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getAsset(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(asset -> ResponseEntity.ok(toAssetView(asset)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/usage")
    public ResponseEntity<List<Map<String, Object>>> getAssetUsage(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(asset -> ResponseEntity.ok(assetReferenceService.findUsageDetails(asset.getFilePath())))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadAsset(@RequestParam("file") MultipartFile file,
                                         @RequestParam("category") String category) {
        try {
            Asset saved = storeNewFile(file, category, file.getOriginalFilename());
            return ResponseEntity.ok(toAssetView(saved));
        } catch (IOException ex) {
            return errorResponse("Failed to upload file: " + ex.getMessage());
        } catch (Exception ex) {
            return errorResponse("Unexpected error: " + ex.getMessage());
        }
    }

    @PutMapping("/{id}/rename")
    public ResponseEntity<Map<String, Object>> renameAsset(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return assetRepository.findById(id)
                .map(asset -> {
                    String newName = body.get("name");
                    if (newName == null || newName.isBlank()) {
                        throw new RuntimeException("Name is required");
                    }
                    asset.setName(newName.trim());
                    return ResponseEntity.ok(toAssetView(assetRepository.save(asset)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/replace")
    public ResponseEntity<?> replaceAsset(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return assetRepository.findById(id)
                .map(asset -> {
                    try {
                        deletePhysicalFile(asset);
                        String extension = extensionOf(file.getOriginalFilename());
                        String newFileName = UUID.randomUUID().toString() + extension;
                        Path targetLocation = this.fileStorageLocation.resolve(newFileName);
                        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                        String oldPath = asset.getFilePath();
                        String newPath = "/api/assets/download/" + newFileName;
                        asset.setFilePath(newPath);
                        asset.setFileSizeBytes(file.getSize());
                        if (file.getOriginalFilename() != null && !file.getOriginalFilename().isBlank()) {
                            asset.setName(file.getOriginalFilename());
                        }
                        Asset saved = assetRepository.save(asset);
                        if (oldPath != null && !oldPath.equals(newPath)) {
                            assetReferenceService.removeAssetReferences(oldPath);
                        }
                        return ResponseEntity.ok(toAssetView(saved));
                    } catch (IOException e) {
                        return errorResponse("Failed to replace file: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadAsset(@PathVariable String fileName) {
        try {
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                String contentType = "application/octet-stream";
                if (fileName.toLowerCase().endsWith(".png")) contentType = "image/png";
                if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) contentType = "image/jpeg";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteAsset(@PathVariable Long id) {
        return assetRepository.findById(id)
                .map(asset -> {
                    String path = asset.getFilePath();
                    assetReferenceService.removeAssetReferences(path);
                    deletePhysicalFile(asset);
                    assetRepository.delete(asset);
                    return ResponseEntity.ok(Map.of("message", "Asset deleted permanently"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Asset storeNewFile(MultipartFile file, String category, String displayName) throws IOException {
        String originalFileName = displayName;
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "unknown";
        }
        String extension = extensionOf(originalFileName);
        String newFileName = UUID.randomUUID().toString() + extension;

        if (!Files.exists(this.fileStorageLocation)) {
            Files.createDirectories(this.fileStorageLocation);
        }

        Path targetLocation = this.fileStorageLocation.resolve(newFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        Asset asset = new Asset();
        asset.setName(originalFileName);
        asset.setCategory(category);
        asset.setFilePath("/api/assets/download/" + newFileName);
        asset.setFileSizeBytes(file.getSize());
        return assetRepository.save(asset);
    }

    private void deletePhysicalFile(Asset asset) {
        try {
            String fileName = asset.getFilePath().substring(asset.getFilePath().lastIndexOf("/") + 1);
            Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> toAssetView(Asset asset) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", asset.getId());
        view.put("name", asset.getName());
        view.put("category", asset.getCategory());
        view.put("filePath", asset.getFilePath());
        view.put("uploadedAt", asset.getUploadedAt());
        view.put("fileSizeBytes", asset.getFileSizeBytes() != null ? asset.getFileSizeBytes() : 0L);
        view.put("usageCount", assetReferenceService.countUsage(asset.getFilePath()));
        return view;
    }

    private String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : "";
    }

    private ResponseEntity<Map<String, String>> errorResponse(String message) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", message);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorResponse);
    }
}
