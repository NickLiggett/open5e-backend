package com.main.app.ownership;

import com.main.app.document.Document;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Base class for every resource entity (creatures, spells, items, …). Each resource belongs to a document, and the
 * visibility filter hides resources whose document the current user can't see.
 */
@Getter
@Setter
@MappedSuperclass
@Filter(name = Visibility.FILTER, condition = Visibility.RESOURCE_CONDITION, deduceAliasInjectionPoints = false)
public abstract class OwnedResource {

    @Id
    @Column(name = "key")
    private String key;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_key", nullable = false)
    private Document document;

    /** The document's key, for filtering without a join. Set {@link #document} to change it. */
    @Column(name = "document_key", insertable = false, updatable = false)
    private String documentKey;

    /** For a customized copy, the key of the resource it was copied from. */
    @Column(name = "derived_from")
    private String derivedFrom;
}
