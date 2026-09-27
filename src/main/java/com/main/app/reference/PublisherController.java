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
@RequestMapping("/api/publishers")
public class PublisherController {

    private final PublisherRepository publisherRepository;
    private final ResourceQueries queries;

    public PublisherController(PublisherRepository publisherRepository, ResourceQueries queries) {
        this.publisherRepository = publisherRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<PublisherDTO> list(PublisherFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(publisherRepository, filter.toSpecification(), pageable, PublisherDTO::from);
    }

    @GetMapping("/{key}")
    public PublisherDTO get(@PathVariable String key) {
        return queries.get(publisherRepository, "publisher", key, PublisherDTO::from);
    }
}
