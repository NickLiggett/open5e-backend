package com.main.app.importer;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** Small custom content files for tests, in the shape the real ones have. */
final class ContentFixtures {

    /** Default content: a document and two species, the second a subspecies of the first. */
    static final String DEFAULT = """
            {"documents": [{"key": "zz-test-book", "name": "Test Book", "display_name": "Test Book", "type": "SOURCE",
                            "publisher": {"key": "wizards-of-the-coast", "name": "Wizards of the Coast"},
                            "gamesystem": {"key": "5e-2014", "name": "5th Edition 2014"}}],
             "species": [{"key": "zz-test-book_a", "document": "zz-test-book", "name": "Alpha", "is_subspecies": false,
                          "desc": "The first.", "traits": [{"name": "Darkvision", "desc": "You see.", "type": null, "order": null,
                          "crossreferences": {"to": []}}], "crossreferences": {"to": []}},
                         {"key": "zz-test-book_b", "document": "zz-test-book", "name": "Beta", "is_subspecies": true,
                          "subspecies_of": "zz-test-book_a", "desc": "The second.", "traits": [], "crossreferences": {"to": []}}]}""";

    /** Private content for the user zz-test-owner: its own document and two species. */
    static final String PRIVATE = """
            {"owner": "zz-test-owner",
             "documents": [{"key": "zz-test-private", "name": "Private Book", "display_name": "Private Book", "type": "SOURCE",
                            "publisher": {"key": "wizards-of-the-coast", "name": "Wizards of the Coast"},
                            "gamesystem": {"key": "5e-2014", "name": "5th Edition 2014"}, "licenses": []}],
             "species": [{"key": "zz-test-private_x", "document": "zz-test-private", "name": "Xander", "is_subspecies": false,
                          "desc": "A secret.", "traits": [{"name": "Hush", "desc": "Quiet.", "type": null, "order": null,
                          "crossreferences": {"to": []}}], "crossreferences": {"to": []}},
                         {"key": "zz-test-private_y", "document": "zz-test-private", "name": "Yara", "is_subspecies": true,
                          "subspecies_of": "zz-test-private_x", "desc": "Another.", "traits": [], "crossreferences": {"to": []}}]}""";

    private ContentFixtures() {
    }

    static Resource file(String name, String json) {
        return new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return name;
            }
        };
    }

    static CustomContent content(JsonMapper mapper, String defaultJson, String... privateJson) {
        List<Resource> shared = defaultJson == null ? List.of() : List.of(file("default.json", defaultJson));
        List<Resource> own = new java.util.ArrayList<>();
        for (int i = 0; i < privateJson.length; i++) {
            own.add(file("private-" + i + ".json", privateJson[i]));
        }
        return new CustomContent(mapper, shared, own);
    }
}
