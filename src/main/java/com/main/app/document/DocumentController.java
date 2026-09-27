package com.main.app.document;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Documents visible to the current user: default content, their own, and those shared with them. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final ResourceQueries queries;

    public DocumentController(DocumentRepository documentRepository, ResourceQueries queries) {
        this.documentRepository = documentRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<DocumentDTO> list(DocumentFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(documentRepository, filter.toSpecification(), pageable, DocumentDTO::from);
    }

    @GetMapping("/{key}")
    public DocumentDTO get(@PathVariable String key) {
        return queries.get(documentRepository, "document", key, DocumentDTO::from);
    }
}
