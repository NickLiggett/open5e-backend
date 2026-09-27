package com.main.app.document;

import com.main.app.common.web.ResourceQueries;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Documents visible to the current user: default content, their own, and those shared with them. Users create their
 * own documents here and share them with other users.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final ResourceQueries queries;
    private final DocumentService documentService;

    public DocumentController(DocumentRepository documentRepository, ResourceQueries queries,
                              DocumentService documentService) {
        this.documentRepository = documentRepository;
        this.queries = queries;
        this.documentService = documentService;
    }

    @GetMapping
    public PagedModel<DocumentDTO> list(DocumentFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(documentRepository, filter.toSpecification(), pageable, DocumentDTO::from);
    }

    @GetMapping("/{key}")
    public DocumentDTO get(@PathVariable String key) {
        return queries.get(documentRepository, "document", key, DocumentDTO::from);
    }

    @PostMapping
    public ResponseEntity<DocumentDTO> create(@RequestBody DocumentRequest request) {
        Document document = documentService.create(request);
        return ResponseEntity.created(URI.create("/api/documents/" + document.getKey())).body(DocumentDTO.from(document));
    }

    @PutMapping("/{key}")
    public DocumentDTO update(@PathVariable String key, @RequestBody DocumentRequest request) {
        return DocumentDTO.from(documentService.update(key, request));
    }

    /** Deletes the document and all of its content. */
    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(@PathVariable String key) {
        documentService.delete(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{key}/members")
    public List<MemberDTO> members(@PathVariable String key) {
        return documentService.members(key);
    }

    /** Shares the document with a user, or changes their role. */
    @PutMapping("/{key}/members/{username}")
    public MemberDTO share(@PathVariable String key, @PathVariable String username, @RequestBody MemberRequest request) {
        return documentService.share(key, username, request);
    }

    @DeleteMapping("/{key}/members/{username}")
    public ResponseEntity<Void> unshare(@PathVariable String key, @PathVariable String username) {
        documentService.unshare(key, username);
        return ResponseEntity.noContent().build();
    }
}
