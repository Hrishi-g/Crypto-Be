// package com.practice.firstapp.service;

// import java.io.IOException;
// import java.util.List;
// import java.util.Map;
// import org.springframework.cache.annotation.Cacheable;
// import org.springframework.stereotype.Service;
// import org.springframework.web.multipart.MultipartFile;
// import com.cloudinary.Cloudinary;
// import com.cloudinary.utils.ObjectUtils;
// import com.practice.firstapp.repo.PhotoRepository;
// import com.practice.firstapp.vo.Photo;

// @Service
// public class PhotoService {

// private PhotoRepository photoRepository;
// private Cloudinary cloudinary;

// public PhotoService(PhotoRepository photoRepository, Cloudinary cloudinary) {
// this.photoRepository = photoRepository;
// this.cloudinary = cloudinary;
// }

// public List<Photo> getAllPhotos() {
// return photoRepository.findAll();
// }

// public String uploadImage(MultipartFile file) throws IOException {
// Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
// ObjectUtils.asMap("type", "authenticated"));
// String publicId = uploadResult.get("public_id").toString();
// Photo photo = new Photo();
// photo.setPublicId(publicId);
// photoRepository.save(photo);
// return uploadResult.get("secure_url").toString();
// }

// @Cacheable(value = "imageUrls", key = "#publicIds")
// public String cloudinaryUrl(String publicIds) { // getches the image by
// signing and authentication
// return cloudinary.url()
// .resourceType("image")
// .secure(true)
// .signed(true)
// .type("authenticated")
// .generate(publicIds);
// }

// public void deletePhoto(Long id) throws IOException {
// // 1. Delete from Cloudinary
// // cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("type",
// // "authenticated"));

// // 2. Delete from Database
// photoRepository.deleteById(id);
// }
// }