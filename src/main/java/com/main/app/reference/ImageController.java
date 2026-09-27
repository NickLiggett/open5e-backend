package com.main.app.reference;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageRepository imageRepository;
    private final ResourceQueries queries;

    public ImageController(ImageRepository imageRepository, ResourceQueries queries) {
        this.imageRepository = imageRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<ImageDTO> list(ImageFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(imageRepository, filter.toSpecification(), pageable, ImageDTO::from);
    }

    @GetMapping("/{key}")
    public ImageDTO get(@PathVariable String key) {
        return queries.get(imageRepository, "image", key, ImageDTO::from);
    }
}
