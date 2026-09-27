package com.main.app.common.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

/**
 * The two read operations every resource endpoint has. Mapping to DTOs happens inside the transaction, so lazy
 * associations (each resource's document) can load.
 */
@Component
@Transactional(readOnly = true)
public class ResourceQueries {

    public <E, D> PagedModel<D> list(JpaSpecificationExecutor<E> repository, Specification<E> filter, Pageable pageable,
                                     Function<E, D> toDto) {
        return new PagedModel<>(repository.findAll(filter, pageable).map(toDto));
    }

    /** @throws ResourceNotFoundException if there is no such resource, or the current user can't see it */
    public <E, D> D get(JpaRepository<E, String> repository, String resource, String key, Function<E, D> toDto) {
        return repository.findById(key).map(toDto).orElseThrow(() -> new ResourceNotFoundException(resource, key));
    }
}
