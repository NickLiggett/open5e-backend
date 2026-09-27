package com.main.app.document;

import com.main.app.common.DocumentSummary;
import com.main.app.common.NamedReference;
import com.main.app.ownership.Visibility;
import com.main.app.ownership.VisibilityUserIdResolver;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.ParamDef;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * A source of content: a default Open5e document (no owner) or a user's own document. Also defines the visibility
 * filter used by every resource entity.
 */
@Getter
@Setter
@Entity
@Table(name = "documents", schema = "open5e")
@FilterDef(
        name = Visibility.FILTER,
        parameters = @ParamDef(name = Visibility.USER_ID, type = Long.class, resolver = VisibilityUserIdResolver.class),
        autoEnabled = true,
        applyToLoadByKey = true)
@Filter(name = Visibility.FILTER, condition = Visibility.DOCUMENT_CONDITION, deduceAliasInjectionPoints = false)
public class Document {

    /** The type of user-created documents; Open5e sources are {@code SOURCE}. */
    public static final String HOMEBREW = "HOMEBREW";

    @Id
    @Column(name = "key")
    private String key;

    @Column(name = "name")
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "\"desc\"")
    private String desc;

    @Column(name = "type")
    private String type;

    @Column(name = "author")
    private String author;

    @Column(name = "publication_date")
    private OffsetDateTime publicationDate;

    @Column(name = "permalink")
    private String permalink;

    @Column(name = "distance_unit")
    private String distanceUnit;

    @Column(name = "weight_unit")
    private String weightUnit;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "publisher")
    private NamedReference publisher;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gamesystem")
    private NamedReference gamesystem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "licenses")
    private List<NamedReference> licenses;

    /** The owning user, or null for default content. */
    @Column(name = "owner_id")
    private Long ownerId;

    public DocumentSummary toSummary() {
        return new DocumentSummary(key, name, type, permalink, publisher, gamesystem, displayName);
    }
}
