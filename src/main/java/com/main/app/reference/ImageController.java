package com.main.app.reference;

import com.main.app.common.web.ResourceQueries;
import com.main.app.ownership.CopyRequest;
import com.main.app.ownership.ResourceType;
import com.main.app.ownership.ResourceWriter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private static final ResourceType<Image, ImageDTO> TYPE =
            new ResourceType<>(Image.class, ImageDTO.class, "/api/images", "image", ImageDTO::from);

    private final ImageRepository imageRepository;
    private final ResourceQueries queries;
    private final ResourceWriter writer;

    public ImageController(ImageRepository imageRepository, ResourceQueries queries, ResourceWriter writer) {
        this.imageRepository = imageRepository;
        this.queries = queries;
        this.writer = writer;
    }

    @GetMapping
    public PagedModel<ImageDTO> list(ImageFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(imageRepository, filter.toSpecification(), pageable, ImageDTO::from);
    }

    @GetMapping("/{key}")
    public ImageDTO get(@PathVariable String key) {
        return queries.get(imageRepository, "image", key, ImageDTO::from);
    }

    @PostMapping
    public ResponseEntity<ImageDTO> create(@RequestBody ObjectNode body) {
        return writer.create(TYPE, body);
    }

    @PutMapping("/{key}")
    public ImageDTO replace(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.replace(TYPE, key, body);
    }

    @PatchMapping("/{key}")
    public ImageDTO update(@PathVariable String key, @RequestBody ObjectNode body) {
        return writer.update(TYPE, key, body);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        writer.delete(TYPE, key);
        return ResponseEntity.noContent().build();
    }

    /** Copies the resource into one of the current user's documents, to customize it. */
    @PostMapping("/{key}/copy")
    public ResponseEntity<ImageDTO> copy(@PathVariable String key, @RequestBody(required = false) CopyRequest request) {
        return writer.copy(TYPE, key, request);
    }
}
