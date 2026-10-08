package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reading custom content files and refusing the mistakes that would let them clash with Open5e's or anyone's content. */
class CustomContentTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    private static final String DOCUMENT = """
            "documents": [{"key": "my-book", "name": "My Book", "display_name": "My Book", "type": "SOURCE",
                           "desc": "not copied into rows", "publisher": {"key": "me", "name": "Me"},
                           "gamesystem": {"key": "5e-2014", "name": "5th Edition 2014"}}]""";

    private CustomContent load(String... files) {
        List<Resource> resources = new java.util.ArrayList<>();
        for (int i = 0; i < files.length; i++) {
            String name = "file-" + i + ".json";
            resources.add(new ByteArrayResource(files[i].getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return name;
                }
            });
        }
        return new CustomContent(mapper, resources);
    }

    private static String file(String... parts) {
        return "{" + String.join(",", parts) + "}";
    }

    @Test
    void readsRowsByEndpoint() {
        CustomContent content = load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": \"my-book\", \"name\": \"A\"}]"));

        assertThat(content.tables()).containsExactlyInAnyOrder("documents", "species");
        assertThat(content.documentKeys()).containsExactly("my-book");
        assertThat(content.size()).isEqualTo(2);
        assertThat(content.rows("species")).hasSize(1);
        assertThat(content.rows("spells")).isEmpty();
    }

    @Test
    void expandsADocumentKeyToTheObjectOpen5eRowsHave() {
        CustomContent content = load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": \"my-book\"}]"));

        ObjectNode document = (ObjectNode) content.rows("species").getFirst().get("document");

        assertThat(document.get("key").asString()).isEqualTo("my-book");
        assertThat(document.get("name").asString()).isEqualTo("My Book");
        assertThat(document.get("display_name").asString()).isEqualTo("My Book");
        assertThat(document.get("publisher").get("key").asString()).isEqualTo("me");
        assertThat(document.get("gamesystem").get("key").asString()).isEqualTo("5e-2014");
        assertThat(document.has("desc")).isFalse(); // a summary, not the whole document
    }

    @Test
    void acceptsADocumentGivenAsAnObject() {
        CustomContent content = load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": {\"key\": \"my-book\"}}]"));

        assertThat(content.rows("species").getFirst().get("document").get("key").asString()).isEqualTo("my-book");
    }

    @Test
    void joinsSeveralFilesAndKeepsTheirOrder() {
        CustomContent content = load(
                file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": \"my-book\"}]"),
                file("\"species\": [{\"key\": \"my-book_b\", \"document\": \"my-book\"}]"));

        assertThat(content.rows("species")).extracting(row -> row.get("key").asString()).containsExactly("my-book_a", "my-book_b");
    }

    @Test
    void givesCopiesSoOneImportCannotChangeAnother() {
        CustomContent content = load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": \"my-book\", \"name\": \"A\"}]"));

        content.rows("species").getFirst().put("name", "changed");

        assertThat(content.rows("species").getFirst().get("name").asString()).isEqualTo("A");
    }

    @Test
    void anEmptySetOfFilesIsNoContent() {
        CustomContent content = load();

        assertThat(content.tables()).isEmpty();
        assertThat(content.documentKeys()).isEmpty();
        assertThat(content.size()).isZero();
    }

    @Test
    void asASourceItDescribesItselfAndGivesItsRows() {
        CustomContent content = load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\", \"document\": \"my-book\"}]"));

        DefaultContentSource source = content.asSource();

        assertThat(source.describe()).contains("custom content").contains("2 rows");
        assertThat(source.fetch("species")).hasSize(1);
        assertThat(source.fetch("spells")).isEmpty();
    }

    @Test
    void refusesWhatItCouldNotApplySafely() {
        assertThatThrownBy(() -> load("[1, 2]")).hasMessageContaining("must be an object");
        assertThatThrownBy(() -> load("{ not json")).hasMessageContaining("can't be read");
        assertThatThrownBy(() -> load(file("\"species\": {}"))).hasMessageContaining("must be an array");
        assertThatThrownBy(() -> load(file(DOCUMENT, "\"species\": [{\"name\": \"no key\", \"document\": \"my-book\"}]")))
                .hasMessageContaining("has no key");
        assertThatThrownBy(() -> load(file(DOCUMENT, "\"species\": [{\"key\": \" \", \"document\": \"my-book\"}]")))
                .hasMessageContaining("has no key");
        assertThatThrownBy(() -> load(file(DOCUMENT, "\"species\": [{\"key\": \"my-book_a\"}]")))
                .hasMessageContaining("has no document");
    }

    @Test
    void refusesKeysUsedTwice() {
        assertThatThrownBy(() -> load(file(DOCUMENT, "\"species\": [{\"key\": \"k\", \"document\": \"my-book\"}, {\"key\": \"k\", \"document\": \"my-book\"}]")))
                .hasMessageContaining("the key 'k' twice");
        assertThatThrownBy(() -> load(file(DOCUMENT), file(DOCUMENT)))
                .hasMessageContaining("the document 'my-book' twice");
    }

    @Test
    void neverAddsToSomeoneElsesDocument() {
        assertThatThrownBy(() -> load(file(DOCUMENT, "\"species\": [{\"key\": \"x\", \"document\": \"srd-2014\"}]")))
                .hasMessageContaining("is in the document 'srd-2014', which these files don't define")
                .hasMessageContaining("can't add to Open5e's documents or anyone's own");
        assertThatThrownBy(() -> load(file("\"species\": [{\"key\": \"x\", \"document\": \"my-book\"}]")))
                .hasMessageContaining("which these files don't define");
    }

    @Test
    void keepsDocumentKeysToCharactersThatAreSafeInSql() {
        for (String bad : new String[]{"My-Book", "a b", "a'b", "a;drop", "-a", ""}) {
            assertThatThrownBy(() -> load(file("\"documents\": [{\"key\": \"" + bad + "\", \"name\": \"x\"}]")))
                    .as(bad).isInstanceOf(IllegalStateException.class);
        }
        assertThat(load(file("\"documents\": [{\"key\": \"a.b_c-1\", \"name\": \"x\"}]")).documentKeys()).containsExactly("a.b_c-1");
    }

    // ----- private content

    private static final String PRIVATE_DOCUMENT = """
            "documents": [{"key": "secret-book", "name": "Secret Book", "display_name": "Secret Book", "type": "SOURCE"}]""";

    private CustomContent loadWithPrivate(List<String> shared, List<String> privateFiles) {
        List<Resource> shares = new java.util.ArrayList<>();
        for (int i = 0; i < shared.size(); i++) {
            shares.add(ContentFixtures.file("shared-" + i + ".json", shared.get(i)));
        }
        List<Resource> own = new java.util.ArrayList<>();
        for (int i = 0; i < privateFiles.size(); i++) {
            own.add(ContentFixtures.file("private-" + i + ".json", privateFiles.get(i)));
        }
        return new CustomContent(mapper, shares, own);
    }

    @Test
    void privateFilesBelongToTheirOwner_andAreNotPartOfTheDefaultContent() {
        CustomContent content = loadWithPrivate(List.of(file(DOCUMENT)), List.of(file("\"owner\": \"nick\"", PRIVATE_DOCUMENT,
                "\"species\": [{\"key\": \"secret-book_s\", \"document\": \"secret-book\"}]")));

        assertThat(content.documentKeys()).containsExactly("my-book");
        assertThat(content.rows("species")).isEmpty();
        assertThat(content.privateContent()).hasSize(1);
        CustomContent.Private one = content.privateContent().getFirst();
        assertThat(one.owner()).isEqualTo("nick");
        assertThat(one.file()).isEqualTo("private-0.json");
        assertThat(one.content().documentKeys()).containsExactly("secret-book");
        assertThat(one.content().rows("species")).hasSize(1);
        assertThat(one.content().rows("species").getFirst().get("document").get("key").asString()).isEqualTo("secret-book");
        assertThat(one.content().rows("species").getFirst().has("owner")).isFalse();
    }

    @Test
    void aPrivateFileNeedsAnOwnerWhoseNameCouldBeAUsername() {
        assertThatThrownBy(() -> loadWithPrivate(List.of(), List.of(file(PRIVATE_DOCUMENT))))
                .hasMessageContaining("needs an \"owner\"");
        for (String bad : new String[]{"Nick", "a b", "", "-x", "x".repeat(33), "a@b.com"}) {
            assertThatThrownBy(() -> loadWithPrivate(List.of(), List.of(file("\"owner\": \"" + bad + "\"", PRIVATE_DOCUMENT))))
                    .as(bad).hasMessageContaining("needs an \"owner\"");
        }
        assertThatThrownBy(() -> loadWithPrivate(List.of(), List.of(file("\"owner\": 7", PRIVATE_DOCUMENT))))
                .hasMessageContaining("needs an \"owner\""); // a number isn't a username
    }

    @Test
    void contentWithAnOwnerCannotBeInTheRepositoryAsDefaultContent() {
        assertThatThrownBy(() -> loadWithPrivate(List.of(file("\"owner\": \"nick\"", DOCUMENT)), List.of()))
                .hasMessageContaining("has an owner, which makes it private")
                .hasMessageContaining("not in the repository");
    }

    @Test
    void aPrivateRowCannotBeInTheDocumentOfAnotherFile() {
        // each private file is on its own: it can't add to the default content's document, or to another private file's
        assertThatThrownBy(() -> loadWithPrivate(List.of(file(DOCUMENT)), List.of(file("\"owner\": \"nick\"",
                "\"species\": [{\"key\": \"x\", \"document\": \"my-book\"}]"))))
                .hasMessageContaining("which these files don't define");
    }

    @Test
    void noDocumentOrKeyIsInTwoPlaces() {
        assertThatThrownBy(() -> loadWithPrivate(List.of(file(DOCUMENT)), List.of(file("\"owner\": \"nick\"", DOCUMENT))))
                .hasMessageContaining("The document 'my-book' is in the default custom content and in private content private-0.json");
        assertThatThrownBy(() -> loadWithPrivate(List.of(), List.of(
                file("\"owner\": \"a\"", PRIVATE_DOCUMENT), file("\"owner\": \"b\"", PRIVATE_DOCUMENT))))
                .hasMessageContaining("The document 'secret-book' is in private content private-0.json and in private content private-1.json");
        assertThatThrownBy(() -> loadWithPrivate(List.of(file(DOCUMENT, "\"species\": [{\"key\": \"k\", \"document\": \"my-book\"}]")),
                List.of(file("\"owner\": \"a\"", PRIVATE_DOCUMENT, "\"species\": [{\"key\": \"k\", \"document\": \"secret-book\"}]"))))
                .hasMessageContaining("The species key 'k' is in the default custom content and in private content private-0.json");
    }

    @Test
    void readsPrivateFilesFromAFolder_andOnlyThoseEndingInJson(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) throws Exception {
        java.nio.file.Files.writeString(folder.resolve("b-book.json"), file("\"owner\": \"nick\"", PRIVATE_DOCUMENT));
        java.nio.file.Files.writeString(folder.resolve("notes.txt"), "not content, and not JSON");
        java.nio.file.Files.writeString(folder.resolve("a-book.json"), file("\"owner\": \"sam\"",
                "\"documents\": [{\"key\": \"other-book\", \"name\": \"Other\"}]"));

        CustomContent content = new CustomContent(mapper, folder.toString());

        assertThat(content.privateContent()).extracting(CustomContent.Private::file).containsExactly("a-book.json", "b-book.json");
        assertThat(content.privateContent()).extracting(CustomContent.Private::owner).containsExactly("sam", "nick");
    }

    @Test
    void noFolderMeansNoPrivateContent_andAFolderThatIsntOneIsAMistake(@org.junit.jupiter.api.io.TempDir java.nio.file.Path folder) {
        assertThat(new CustomContent(mapper, "").privateContent()).isEmpty();
        assertThat(new CustomContent(mapper, "  ").privateContent()).isEmpty();
        assertThat(new CustomContent(mapper, folder.toString()).privateContent()).isEmpty();

        // Not read until asked for, so a bad folder can't stop the app starting
        CustomContent notAFolder = new CustomContent(mapper, folder.resolve("nope").toString());
        assertThatThrownBy(notAFolder::privateContent).hasMessageContaining("isn't a folder");
    }
}
