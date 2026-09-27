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
@RequestMapping("/api/alignments")
public class AlignmentController {

    private final AlignmentRepository alignmentRepository;
    private final ResourceQueries queries;

    public AlignmentController(AlignmentRepository alignmentRepository, ResourceQueries queries) {
        this.alignmentRepository = alignmentRepository;
        this.queries = queries;
    }

    @GetMapping
    public PagedModel<AlignmentDTO> list(AlignmentFilter filter, @PageableDefault(sort = "key") Pageable pageable) {
        return queries.list(alignmentRepository, filter.toSpecification(), pageable, AlignmentDTO::from);
    }

    @GetMapping("/{key}")
    public AlignmentDTO get(@PathVariable String key) {
        return queries.get(alignmentRepository, "alignment", key, AlignmentDTO::from);
    }
}
