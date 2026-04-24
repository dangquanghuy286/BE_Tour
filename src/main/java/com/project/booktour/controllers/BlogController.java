package com.project.booktour.controllers;

import com.project.booktour.dtos.BlogDTO;
import com.project.booktour.exceptions.DataNotFoundException;
import com.project.booktour.exceptions.InvalidParamException;
import com.project.booktour.models.Blog;
import com.project.booktour.responses.BlogListResponse;
import com.project.booktour.responses.ErrorResponse;
import com.project.booktour.services.BlogService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("${api.prefix}/blogs")
public class BlogController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private ModelMapper modelMapper;

    // Chuyển đổi Blog thành BlogDTO với URL ảnh đầy đủ
    private BlogDTO convertToBlogDTO(Blog blog) {
        BlogDTO blogDTO = modelMapper.map(blog, BlogDTO.class);
        if (blog.getImage() != null && !blog.getImage().isEmpty()) {
            String fullImageUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/blogs/images/")
                    .path(blog.getImage())
                    .toUriString();
            blogDTO.setImage(fullImageUrl);
        }
        return blogDTO;
    }

    // Tạo mới blog
    @PostMapping
    public ResponseEntity<?> createBlog(@RequestBody BlogDTO blogDTO) {
        try {
            if (blogDTO == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "Dữ liệu blog không được null", 400));
            }

            Blog blog = modelMapper.map(blogDTO, Blog.class);
            Blog savedBlog = blogService.createBlog(blog);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToBlogDTO(savedBlog));

        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi không xác định khi tạo blog: " + e.getMessage(), 500));
        }
    }

    // Lấy tất cả blog với phân trang và sắp xếp
    @GetMapping
    public ResponseEntity<?> getAllBlogs(@RequestParam Map<String, String> params) {
        try {
            // Validate và lấy parameters
            int page = parseIntParam(params.getOrDefault("page", "0"), "page");
            int limit = parseIntParam(params.getOrDefault("limit", "10"), "limit");

            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Page phải >= 0", 400));
            }
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Limit phải > 0 và <= 100", 400));
            }

            // Validate sorting parameters
            String sortBy = params.getOrDefault("sortBy", "createdAt");
            String sortDir = params.getOrDefault("sortDir", "desc");

            List<String> validSortFields = List.of("createdAt", "title", "author");
            if (!validSortFields.contains(sortBy)) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter",
                                "sortBy không hợp lệ. Chỉ chấp nhận: " + validSortFields, 400));
            }

            if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "sortDir phải là 'asc' hoặc 'desc'", 400));
            }

            // Tạo Pageable với sắp xếp
            Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
            PageRequest pageRequest = PageRequest.of(page, limit, sort);

            // Gọi service để lấy danh sách blog
            Page<Blog> blogPage = blogService.getAllBlogs(pageRequest);
            List<BlogDTO> blogDTOs = blogPage.getContent().stream()
                    .map(this::convertToBlogDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(new BlogListResponse(blogDTOs, blogPage.getTotalPages(), blogPage.getTotalElements()));

        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", "Tham số số không hợp lệ: " + e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi khi lấy danh sách blog: " + e.getMessage(), 500));
        }
    }

    // Lấy blog theo ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getBlogById(@PathVariable Integer id) {
        try {
            Optional<Blog> blog = blogService.getBlogById(id);
            if (blog.isPresent()) {
                return ResponseEntity.ok(convertToBlogDTO(blog.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorResponse("Not Found", "Không tìm thấy blog với ID: " + id, 404));
            }

        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi khi lấy blog: " + e.getMessage(), 500));
        }
    }

    // Cập nhật blog
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBlog(@PathVariable Integer id, @RequestBody BlogDTO blogDTO) {
        try {
            if (blogDTO == null) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Bad Request", "Dữ liệu blog không được null", 400));
            }

            Blog blogDetails = modelMapper.map(blogDTO, Blog.class);
            Blog updatedBlog = blogService.updateBlog(id, blogDetails);
            return ResponseEntity.ok(convertToBlogDTO(updatedBlog));

        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi khi cập nhật blog: " + e.getMessage(), 500));
        }
    }

    // Xóa blog
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBlog(@PathVariable Integer id) {
        try {
            blogService.deleteBlog(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa blog thành công");
            return ResponseEntity.ok(response);

        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi khi xóa blog: " + e.getMessage(), 500));
        }
    }

    // Upload ảnh cho blog
    @PutMapping("/{id}/image")
    public ResponseEntity<?> updateBlogImage(
            @PathVariable Integer id,
            @RequestParam("image") MultipartFile image) {
        try {
            Blog updatedBlog = blogService.updateBlogImage(id, image);
            return ResponseEntity.ok(convertToBlogDTO(updatedBlog));

        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", e.getMessage(), 404));
        } catch (InvalidParamException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("Invalid Parameter", e.getMessage(), 400));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("File Error", "Lỗi xử lý file: " + e.getMessage(), 500));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Internal Server Error",
                            "Lỗi khi cập nhật ảnh blog: " + e.getMessage(), 500));
        }
    }

    // Xem ảnh blog
    @GetMapping("/images/{imageName}")
    public ResponseEntity<?> viewBlogImage(@PathVariable String imageName) {
        try {
            if (imageName == null || imageName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Invalid Parameter", "Tên ảnh không hợp lệ", 400));
            }

            Path imagePath = Paths.get("Uploads/blog_images/" + imageName);
            UrlResource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(resource);
            } else {
                // Trả về ảnh mặc định
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(new UrlResource("https://images.icon-icons.com/1378/PNG/512/avatardefault_92824.png"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Not Found", "Không tìm thấy ảnh: " + e.getMessage(), 404));
        }
    }

    // Helper method để parse integer parameters
    private int parseIntParam(String param, String paramName) throws NumberFormatException {
        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Tham số '" + paramName + "' không hợp lệ: " + param);
        }
    }
}