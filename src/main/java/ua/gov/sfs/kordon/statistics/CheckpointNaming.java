package ua.gov.sfs.kordon.statistics;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.util.Collections.singletonList;

public class CheckpointNaming {

    private final Map<String, String> checkpointNames = new HashMap<>();
    private File checkpointNamesFile;

    private CheckpointNaming() {
    }

    public static CheckpointNaming initialize(final File baseDir) {
        final CheckpointNaming checkpointNaming = new CheckpointNaming();

        try {
            checkpointNaming.checkpointNamesFile = new File(baseDir.getAbsolutePath(), "назви пунктів пропуску.csv");
            if (checkpointNaming.checkpointNamesFile.createNewFile()) {
                Files.write(checkpointNaming.checkpointNamesFile.toPath(), singletonList("Оригінальна назва;Скорочення"));
            } else {
                final List<String> lines = Files.readAllLines(checkpointNaming.checkpointNamesFile.toPath());
                for (int i = 1; i < lines.size(); i++) {
                    final String line = lines.get(i);
                    final int delimiterIndex = line.indexOf(";");
                    checkpointNaming.checkpointNames.put(
                            line.substring(0, delimiterIndex),
                            line.substring(delimiterIndex + 1));
                }
            }
        } catch (Exception e) {
            System.out.printf("Could not create/read `назви пунктів пропуску.csv`. %s", e);
        }

        return checkpointNaming;
    }

    public String getCheckpointShortName(final String checkpointName) {
        final String knownShortName = checkpointNames.get(checkpointName);
        if (knownShortName != null) {
            return knownShortName;
        }
        if (checkpointName.startsWith("Пункт пропуску ")) {
            String shortName = checkpointName.substring(15);
            if (shortName.contains(".")) {
                shortName = shortName.substring(0, shortName.indexOf("."));
            } else if (shortName.contains(" митного поста")) {
                shortName = shortName.substring(0, shortName.indexOf(" митного поста"));
            }
            shortName = shortName.replaceAll("\"", "");
            shortName = shortName.replaceAll("„", "");
            shortName = shortName.replaceAll("”", "");
            shortName = ensureUniqueness(shortName);
            saveCheckpointName(checkpointName, shortName);
            return shortName;
        }
        System.out.printf("Could not shorten %s", checkpointName);
        return checkpointName;
    }

    private String ensureUniqueness(final String shortName) {
        final long sameShortNamesCount = checkpointNames.values().stream()
                .filter(v -> v.contains(shortName))
                .count();
        if (sameShortNamesCount > 0) {
            return shortName + "-" + sameShortNamesCount;
        }
        return shortName;
    }

    private void saveCheckpointName(final String checkpointName, final String shortName) {
        checkpointNames.put(checkpointName, shortName);
        try {
            Files.write(checkpointNamesFile.toPath(),
                    singletonList(String.join(";", checkpointName, shortName)),
                    APPEND);
        } catch (Exception e) {
            System.out.printf("Could not write to `назви пунктів пропуску.csv`. %s", e);
        }
    }
}
