package com.project.booktour.responses;

import com.project.booktour.dtos.BlogDTO;
import java.util.List;

public class BlogListResponse {
    private List<BlogDTO> blogs;
    private int totalPages;
    private long totalElements;

    public BlogListResponse(List<BlogDTO> blogs, int totalPages, long totalElements) {
        this.blogs = blogs;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<BlogDTO> getBlogs() {
        return blogs;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}