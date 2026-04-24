package com.project.booktour.services;

import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.Blog;
import com.project.booktour.repositories.BlogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Service
public class BlogService {

    @Autowired
    private BlogRepository blogRepository;

    private static final String BLOG_IMAGE_UPLOAD_DIR = "Uploads/blog_images/";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

    // Tạo mới blog
    public Blog createBlog(Blog blog) throws InvalidParamException {
        validateBlogData(blog);
        try {
            return blogRepository.save(blog);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lưu blog: " + e.getMessage(), e);
        }
    }

    // Lấy tất cả blog với phân trang
    public Page<Blog> getAllBlogs(Pageable pageable) {
        try {
            return blogRepository.findAll(pageable);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách blog: " + e.getMessage(), e);
        }
    }

    // Lấy blog theo ID
    public Optional<Blog> getBlogById(Integer id) throws InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("ID blog không hợp lệ");
        }

        try {
            return blogRepository.findById(id);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy blog với ID " + id + ": " + e.getMessage(), e);
        }
    }

    // Cập nhật blog
    public Blog updateBlog(Integer id, Blog blogDetails) throws DataNotFoundException, InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("ID blog không hợp lệ");
        }

        validateBlogData(blogDetails);

        try {
            return blogRepository.findById(id)
                    .map(existingBlog -> {
                        existingBlog.setTitle(blogDetails.getTitle());
                        existingBlog.setContent(blogDetails.getContent());
                        existingBlog.setAuthor(blogDetails.getAuthor());
                        if (blogDetails.getImage() != null && !blogDetails.getImage().isEmpty()) {
                            existingBlog.setImage(blogDetails.getImage());
                        }
                        return blogRepository.save(existingBlog);
                    })
                    .orElseThrow(() -> new DataNotFoundException("Blog không tồn tại với ID: " + id));
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi cập nhật blog: " + e.getMessage(), e);
        }
    }

    // Xóa blog
    public void deleteBlog(Integer id) throws DataNotFoundException, InvalidParamException {
        if (id == null || id <= 0) {
            throw new InvalidParamException("ID blog không hợp lệ");
        }

        try {
            if (blogRepository.existsById(id)) {
                blogRepository.deleteById(id);
            } else {
                throw new DataNotFoundException("Blog không tồn tại với ID: " + id);
            }
        } catch (DataNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa blog: " + e.getMessage(), e);
        }
    }

    // Upload ảnh cho blog
    public Blog updateBlogImage(Integer blogId, MultipartFile image) throws DataNotFoundException, InvalidParamException, IOException {
        if (blogId == null || blogId <= 0) {
            throw new InvalidParamException("ID blog không hợp lệ");
        }

        if (image == null || image.isEmpty()) {
            throw new InvalidParamException("File ảnh không được để trống");
        }

        try {
            Blog existingBlog = blogRepository.findById(blogId)
                    .orElseThrow(() -> new DataNotFoundException("Không tìm thấy blog với id: " + blogId));

            String fileName = storeFile(image);
            if (fileName != null) {
                existingBlog.setImage(fileName);
            }

            return blogRepository.save(existingBlog);
        } catch (DataNotFoundException | InvalidParamException | IOException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi không xác định khi cập nhật ảnh blog: " + e.getMessage(), e);
        }
    }

    // Validate dữ liệu blog
    private void validateBlogData(Blog blog) throws InvalidParamException {
        if (blog == null) {
            throw new InvalidParamException("Dữ liệu blog không được null");
        }

        if (blog.getTitle() == null || blog.getTitle().trim().isEmpty()) {
            throw new InvalidParamException("Tiêu đề blog không được để trống");
        }

        if (blog.getTitle().length() > 255) {
            throw new InvalidParamException("Tiêu đề blog không được vượt quá 255 ký tự");
        }

        if (blog.getContent() == null || blog.getContent().trim().isEmpty()) {
            throw new InvalidParamException("Nội dung blog không được để trống");
        }

        if (blog.getAuthor() == null || blog.getAuthor().trim().isEmpty()) {
            throw new InvalidParamException("Tác giả blog không được để trống");
        }

        if (blog.getAuthor().length() > 100) {
            throw new InvalidParamException("Tên tác giả không được vượt quá 100 ký tự");
        }
    }

    // Lưu file với validation
    private String storeFile(MultipartFile file) throws IOException, InvalidParamException {
        if (file.isEmpty()) {
            throw new InvalidParamException("File không được để trống");
        }

        // Kiểm tra kích thước file
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidParamException("Kích thước ảnh không được vượt quá 5MB");
        }

        // Kiểm tra loại file
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidParamException("File phải là hình ảnh (jpg, png, gif, etc.)");
        }

        // Kiểm tra tên file
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new InvalidParamException("Tên file không hợp lệ");
        }

        try {
            String fileName = UUID.randomUUID() + "-" + originalFilename;
            Path uploadPath = Paths.get(BLOG_IMAGE_UPLOAD_DIR);

            // Tạo thư mục nếu chưa tồn tại
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return fileName;
        } catch (IOException e) {
            throw new IOException("Lỗi khi lưu file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi không xác định khi lưu file: " + e.getMessage(), e);
        }
    }
}