// package com.practice.firstapp.controller;

// import java.util.List;
// import org.springframework.http.ResponseEntity;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PostMapping;
// import org.springframework.web.bind.annotation.DeleteMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.bind.annotation.RestController;
// import org.springframework.web.multipart.MultipartFile;
// import com.practice.firstapp.service.PhotoService;
// import com.practice.firstapp.vo.Photo;

// @RestController
// public class PhotoController {

// private PhotoService photoService;

// public PhotoController(PhotoService photoService) {
// this.photoService = photoService;
// }

// @GetMapping("/api/album")
// public List<Photo> getAlbum() {
// return photoService.getAllPhotos();
// }

// @PostMapping("/api/upload")
// public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile
// file) throws java.io.IOException {
// String url = photoService.uploadImage(file);
// return ResponseEntity.ok(java.util.Map.of("message", "Image uploaded
// successfully", "url", url));
// }

// @GetMapping("/api/get-secure-image")
// public ResponseEntity<List<java.util.Map<String, Object>>> getSecureUrl() {
// List<Photo> photos = photoService.getAllPhotos();
// List<java.util.Map<String, Object>> result = photos.stream()
// .map(photo -> {
// java.util.Map<String, Object> map = new java.util.HashMap<>();
// map.put("id", photo.getId());
// map.put("url", photoService.cloudinaryUrl(photo.getPublicId()));
// return map;
// })
// .toList();
// return ResponseEntity.ok(result);
// }

// @DeleteMapping("/api/photo/{id}")
// public ResponseEntity<?> deletePhoto(@PathVariable Long id) throws
// java.io.IOException {
// photoService.deletePhoto(id);
// return ResponseEntity.ok(java.util.Map.of("message", "Image deleted
// successfully"));
// }
// }
