package com.tictactoe.persistence;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves where the application keeps its data files.
 *
 * <p>Data goes in a {@code data} folder at the project root, so the files are found no matter
 * which folder the game was launched from. The project root is the parent of the folder that
 * holds the compiled classes ({@code bin/} from run.sh and run.bat, {@code target/classes} from
 * Maven) or the folder containing the jar. Set {@code -Dtictactoe.data.dir=<folder>} to override.
 */
public final class AppPaths {

    /** System property that overrides the data folder. */
    public static final String DATA_DIR_PROPERTY = "tictactoe.data.dir";

    private AppPaths() {
    }

    /**
     * Returns the absolute data folder.
     *
     * @return the folder that holds the app's data files
     */
    public static Path dataDir() {
        String override = System.getProperty(DATA_DIR_PROPERTY);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath();
        }
        try {
            Path codeLocation = Paths.get(AppPaths.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path projectRoot = codeLocation.getParent();
            if (projectRoot != null) {
                return projectRoot.resolve("data");
            }
        } catch (Exception e) {
            // Code source unavailable (unusual runtime): fall back to the working folder
        }
        return Paths.get("data").toAbsolutePath();
    }

    /**
     * Returns the absolute path of a named file inside the data folder.
     *
     * @param fileName file name, such as {@code tictactoe.db}
     * @return the full path inside the data folder
     */
    public static Path dataFile(String fileName) {
        return dataDir().resolve(fileName);
    }
}
