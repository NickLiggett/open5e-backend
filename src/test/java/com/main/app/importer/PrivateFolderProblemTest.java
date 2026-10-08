package com.main.app.importer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A problem with the private folder must not stop the app starting (the web app never uses it); it only fails an import,
 * with a message that says what to do. (Once, a folder the app's user couldn't read took the whole site down.)
 */
class PrivateFolderProblemTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void creatingTheContentNeverReadsTheFolder(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("broken.json"), "{ not json");

        assertThatCode(() -> new CustomContent(mapper, folder.toString())).doesNotThrowAnyException();
        assertThatCode(() -> new CustomContent(mapper, folder.resolve("missing").toString())).doesNotThrowAnyException();
    }

    @Test
    void aBrokenFileFailsOnlyWhenTheContentIsAskedFor(@TempDir Path folder) throws Exception {
        Files.writeString(folder.resolve("broken.json"), "{ not json");
        CustomContent content = new CustomContent(mapper, folder.toString());

        assertThatThrownBy(content::privateContent).isInstanceOf(RuntimeException.class);
        assertThat(content.size()).isZero(); // the default content is unaffected
    }

    @Test
    void aFolderThatCannotBeListedSaysWhatToDo(@TempDir Path folder) {
        // A file where the folder should be listed fails the same way an unreadable folder does for the app's user
        Path file = folder.resolve("a-file");
        assertThatCode(() -> Files.writeString(file, "x")).doesNotThrowAnyException();

        assertThatThrownBy(() -> new CustomContent(mapper, file.toString()).privateContent())
                .hasMessageContaining("isn't a folder");
    }
}
