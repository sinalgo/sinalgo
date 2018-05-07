package sinalgo.io;

import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class IOUtils {

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Creates a directory if it does not already exist.
     *
     * @param dir The directory path
     * @throws SinalgoFatalException if the folder structure cannot be created
     */
    public static void createDir(String dir) {
        try {
            Files.createDirectories(Paths.get(dir));
        } catch (IOException e) {
            throw new SinalgoFatalException("Failed to create the folder structure '" + dir + "'.", e);
        }
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Open's a project's temporary configuration file. It searches
     * the temporary files folder to check if there's a .run file there, then the
     * user's folder to check if there's a config file there and, in case there
     * isn't, it loads the default from the resources.
     *
     * @param projectName The project's name
     * @throws SinalgoFatalException if the file cannot be read
     */
    public static BufferedInputStream getProjectTempConfigurationAsStream(String projectName) {
        Path tempConfigFilePath = Paths.get(getAsPath(Configuration.getAppTmpFolder(), Configuration.getUserProjectsPackage(),
                projectName, Configuration.getConfigfileFileName() + ".run"));
        if (Files.isReadable(tempConfigFilePath)) {
            try {
                return new BufferedInputStream(Files.newInputStream(tempConfigFilePath));
            } catch (IOException e) {
                throw new SinalgoFatalException("Failed to read temporary project configuration file '" + tempConfigFilePath + "'.", e);
            }
        }
        return getProjectConfigurationAsStream(projectName);
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Open's a project's temporary configuration file. It searches
     * the temporary files folder to check if there's a .run file there, then the
     * user's folder to check if there's a config file there and, in case there
     * isn't, it loads the default from the resources.
     *
     * @param projectName The project's name
     * @throws SinalgoFatalException if the file cannot be read
     */
    public static LineNumberReader getProjectTempConfigurationAsReader(String projectName) {
        Path tempConfigFilePath = Paths.get(getAsPath(Configuration.getAppTmpFolder(), Configuration.getUserProjectsPackage(),
                projectName, Configuration.getConfigfileFileName() + ".run"));
        if (Files.isReadable(tempConfigFilePath)) {
            try {
                return new LineNumberReader(new InputStreamReader(Files.newInputStream(tempConfigFilePath)));
            } catch (IOException e) {
                throw new SinalgoFatalException("Failed to read temporary project configuration file '" + tempConfigFilePath + "'.", e);
            }
        }
        return getProjectConfigurationAsReader(projectName);
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Open's a project's configuration file. It searches the user's
     * folder to check if there's one there and, in case there isn't, it loads the
     * default from the resources.
     *
     * @param projectName The project's name
     * @throws SinalgoFatalException if the file cannot be read
     */
    public static BufferedInputStream getProjectConfigurationAsStream(String projectName) {
        Path userConfigFilePath = Paths.get(getAsPath(Configuration.getAppConfigDir(), Configuration.getUserProjectsPackage(),
                projectName, Configuration.getConfigfileFileName()));
        if (Files.isReadable(userConfigFilePath)) {
            try {
                return new BufferedInputStream(Files.newInputStream(userConfigFilePath));
            } catch (IOException e) {
                throw new SinalgoFatalException("Failed to read project configuration file '" + userConfigFilePath + "'.", e);
            }
        }
        try {
            return getResourceAsStream(getAsPath(Configuration.getProjectResourceDirPrefix(), projectName, Configuration.getConfigfileFileName()));
        } catch (SinalgoFatalException e) {
            throw new SinalgoFatalException("Failed to read the default configuration file for project '" + projectName + "''");
        }
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Open's a project's configuration file. It searches the user's
     * folder to check if there's one there and, in case there isn't, it loads the
     * default from the resources.
     *
     * @param projectName The project's name
     * @throws SinalgoFatalException if the file cannot be read
     */
    public static LineNumberReader getProjectConfigurationAsReader(String projectName) {
        Path userConfigFilePath = Paths.get(getAsPath(Configuration.getAppConfigDir(), Configuration.getUserProjectsPackage(),
                projectName, Configuration.getConfigfileFileName()));
        if (Files.isReadable(userConfigFilePath)) {
            try {
                return new LineNumberReader(new InputStreamReader((Files.newInputStream(userConfigFilePath))));
            } catch (IOException e) {
                throw new SinalgoFatalException("Failed to read project configuration file '" + userConfigFilePath + "'.", e);
            }
        }
        try {
            return getResourceAsReader(getAsPath(Configuration.getProjectResourceDirPrefix(), projectName, Configuration.getConfigfileFileName()));
        } catch (SinalgoFatalException e) {
            throw new SinalgoFatalException("Failed to read the default configuration file for project '" + projectName + "''");
        }
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Loads a resource as a input stream.
     *
     * @param path The path to the resource file
     * @throws SinalgoFatalException if the resource cannot be read
     */
    public static BufferedInputStream getResourceAsStream(String path) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        InputStream resource = cldr.getResourceAsStream(path);
        if (resource == null) {
            throw new SinalgoFatalException("Failed to read resource '" + path + "'.");
        }
        return new BufferedInputStream(resource);
    }

    /**
     * <b>This member is framework internal and should not be used by the project
     * developer.</b> Loads a resource as a line number reader.
     *
     * @param path The path to the resource file
     * @throws SinalgoFatalException if the resource cannot be read
     */
    public static LineNumberReader getResourceAsReader(String path) {
        ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        InputStream resource = cldr.getResourceAsStream(path);
        if (resource == null) {
            throw new SinalgoFatalException("Failed to read resource '" + path + "'.");
        }
        return new LineNumberReader(new InputStreamReader(resource));
    }

    /**
     * Gets the joined elements as a path string.
     * <br/><br/>
     * Ex.: 'root, sinalgo, test' returns 'root/sinalgo/test'
     *
     * @param strings Each elements of the path.
     * @return The path elements joined as a path string.
     */
    public static String getAsPath(String... strings) {
        return String.join("/", strings);
    }

    /**
     * Gets the joined elements as a package string.
     * <br/><br/>
     * Ex.: 'root, sinalgo, test' returns 'root.sinalgo.test'
     *
     * @param strings Each elements of the package.
     * @return The path elements joined as a package string.
     */
    public static String getAsPackage(String... strings) {
        return String.join(".", strings);
    }

    /**
     * Transforms a package string to a path string.
     *
     * @param pkg The package string.
     * @return A path string to the corresponding package.
     */
    public static String toPath(String pkg) {
        return pkg.replace('.', '/');
    }

    /**
     * Transforms a path string to a package string.
     *
     * @param path The path string.
     * @return A package string to the corresponding path.
     */
    public static String toPackage(String path) {
        return path.replace('/', '.');
    }

}
