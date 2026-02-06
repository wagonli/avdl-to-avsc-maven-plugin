package io.jonasg.avro;

import org.apache.commons.io.file.DeletingPathVisitor;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.apache.commons.io.file.Counters.noopPathCounters;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AvdlToAvscConverterTest {

    private Log log;

    private AvdlToAvscConverter converter;
    private Path tempOutputDir;

    @BeforeEach
    void setUp() throws Exception {
        log = mock(Log.class);
        converter = new AvdlToAvscConverter(log);
        tempOutputDir = Files.createTempDirectory("avdl-output");
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @AfterEach
    void cleanUp() throws Exception {
        Files.walkFileTree(tempOutputDir, new DeletingPathVisitor(noopPathCounters()));
    }

    @Test
    void convertWithNonExistentDirectory() {
        String nonExistentDirectory = "non-existent-dir";
        String outputDirectory = "output-dir";

        converter.convert(configuration(nonExistentDirectory, outputDirectory, false));

        verify(log).error("Avdl directory does not exist: " + Path.of(nonExistentDirectory));
    }

    @Test
    void convertWithValidAvdlFiles() {


        converter.convert(configuration("src/test/resources/avdl-input", tempOutputDir.toString(), false));

        verify(log).info(contains("Converted src/test/resources/avdl-input/test.avdl"));
        assertThat(tempOutputDir.toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .contains("Person.avsc");
    }


    @Test
    void should_maintain_directory_structure() {
        converter.convert(configuration("src/test/resources/avdl-maintain-dir", tempOutputDir.toString(), true));

        verify(log).info(contains("Converted src/test/resources/avdl-maintain-dir/deep/dir/test.avdl"));
        verify(log).info(contains("Converted src/test/resources/avdl-maintain-dir/deep/dir2/test.avdl"));
        verify(log).info(contains("Converted src/test/resources/avdl-maintain-dir/test.avdl"));
        assertThat(tempOutputDir.toFile().listFiles())
                .hasSize(2)
                .extracting(File::getName)
                .contains("Person.avsc", "deep");
        assertThat(tempOutputDir.resolve(Path.of("deep/dir")).toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .contains("Person.avsc");
        assertThat(tempOutputDir.resolve(Path.of("deep/dir2")).toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .contains("Person.avsc");

    }


    @Test
    void mainSchemaOnly() {

        converter.convert(configuration("src/test/resources/avdl-main-only", tempOutputDir.toString(), false));

        assertThat(tempOutputDir.toFile().listFiles())
                .hasSize(1)
                .extracting(File::getName)
                .contains("Person.avsc");
    }

    @Test
    void testConvertWithNoAvdlFiles() throws IOException {
        converter.convert(configuration("src/test/resources/no-avdl-files", "src/test/resources/avdl-output", false));

        verify(log, never()).info(contains("Converted"));
    }

    private ConverterConfiguration configuration(String avdlDirectory, String avscDirectory, boolean maintainDirectoryStructure) {
        return new ConverterConfiguration(Path.of(avdlDirectory), Path.of(avscDirectory), maintainDirectoryStructure);
    }
}