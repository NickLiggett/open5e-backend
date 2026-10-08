package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Open5e's rows and the repository's own, as one source for the importer. */
class LayeredContentSourceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private CustomContent custom(String json) {
        List<Resource> files = List.of(new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "custom.json";
            }
        });
        return new CustomContent(mapper, files);
    }

    private static final String ONE_SPECIES = """
            {"documents": [{"key": "my-book", "name": "My Book"}],
             "species": [{"key": "my-book_a", "document": "my-book", "name": "A"}]}""";

    private DefaultContentSource open5e(String... speciesKeys) {
        return new DefaultContentSource() {
            @Override
            public List<ObjectNode> fetch(String endpoint) {
                List<ObjectNode> rows = new ArrayList<>();
                if (endpoint.equals("species")) {
                    for (String key : speciesKeys) {
                        rows.add(mapper.createObjectNode().put("key", key));
                    }
                }
                return rows;
            }

            @Override
            public String describe() {
                return "Open5e (test)";
            }
        };
    }

    @Test
    void addsTheCustomRowsAfterOpen5es() {
        LayeredContentSource source = new LayeredContentSource(open5e("srd_dwarf", "srd_elf"), custom(ONE_SPECIES));

        assertThat(source.fetch("species")).extracting(row -> row.get("key").asString())
                .containsExactly("srd_dwarf", "srd_elf", "my-book_a");
        assertThat(source.fetch("documents")).extracting(row -> row.get("key").asString()).containsExactly("my-book");
    }

    @Test
    void leavesAnEndpointOnlyOpen5eHasAlone() {
        LayeredContentSource source = new LayeredContentSource(open5e("srd_dwarf"), custom(ONE_SPECIES));

        assertThat(source.fetch("spells")).isEmpty();
    }

    @Test
    void refusesAKeyOpen5eHasToo() {
        LayeredContentSource source = new LayeredContentSource(open5e("srd_dwarf", "my-book_a"), custom(ONE_SPECIES));

        assertThatThrownBy(() -> source.fetch("species"))
                .hasMessageContaining("'my-book_a'")
                .hasMessageContaining("same key as one from Open5e (test)");
    }

    @Test
    void saysWhereTheContentComesFrom() {
        LayeredContentSource source = new LayeredContentSource(open5e(), custom(ONE_SPECIES));

        assertThat(source.describe()).isEqualTo("Open5e (test) + custom content (2 rows)");
    }
}
