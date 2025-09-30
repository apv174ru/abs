import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


/**
 * Развертывание приложений в филиалах
 */
public class DFilTask extends Task {

    private static final String DIR_PROGRAM = "/program";
    private static final String DIR_OLD = "/old";
    private static final String DIR_XPP190 = "/xpp190";

    private String errorLog;
    private Properties config;
    private String file;
    private String path;
    private String errorProperty;
    private String guineaPigs;
    private boolean xpp182 = true;
    private boolean xpp190 = true;

    
    public void setConfig(String configFile) {
        config = new Properties();
        try {
            String content = new String(Files.readAllBytes(Paths.get(configFile)), "Cp866");
            config.load(new StringReader(content.replace("\\", "/")));
        } catch (IOException e) {
            config = null;
            logMessage(e.getMessage(), Project.MSG_ERR);
        }
    }

    
    public void setFile(String file) {
        this.file = file;
    }

    
    public void setPath(String progfilPath) {
        path = progfilPath;
    }

    public void setErrorProperty(String errorProperty) {
        this.errorProperty = errorProperty;
    }

    
    public void setXpp182(boolean xpp182) {
        this.xpp182 = xpp182;
    }


    public void setXpp190(boolean xpp190) {
        this.xpp190 = xpp190;
    }

    
    public void setTesterConf(String testerConf) {
        guineaPigs = testerConf;
    }

    
    @Override
    public void execute() throws BuildException {
        try {
            errorLog = "";
            ensureAttributesSpecified();
            backupCurrentVersion();
            deployInFilials();
        } finally {
            if (errorProperty != null) {
                // property must exist even if errorlog is empty
                getProject().setProperty(errorProperty, errorLog);
                if (errorLog.length() > 0) {
                    throw new BuildException("");
                }
            } 
        }
    }

    
    private void ensureAttributesSpecified() {
        if (config == null) {
            throw new BuildException("\"config\" attribute required");
        }
        if (file == null) {
            throw new BuildException("\"file\" attribute required");
        }
        if (path == null) {
            throw new BuildException("\"path\" attribute required");
        }
    }

    
    private void backupCurrentVersion() throws BuildException {
        logMessage("Backup file " + file, Project.MSG_VERBOSE);
        final String fromDir = path + DIR_PROGRAM;
        final String toDir = path + DIR_PROGRAM + DIR_OLD;
        if (!copy190(fromDir, toDir) ||
            !copy182(fromDir, toDir)) {
            throw new BuildException("Error while copying");
        }
    }

    
    private void deployInFilials() {
        final String fromDir = readProperty("work.put") + DIR_PROGRAM;
        for (final String conf : readProperty("conf").split(";")) {
            final String toDir = toDir(conf);
            if (toDir != null) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (guineaPigs == null
                            || guineaPigs.contains(conf)
                            || "work".equals(conf)) {
                            copy190(fromDir, toDir);
                        }
                        copy182(fromDir, toDir);
                    }
                }).run();
            }
        }
    }


    private String readProperty(String key) {
        final String value = config.getProperty(key);
        if (value == null) {
            final String msg = '"' + key + "\" property not found";
            logProperty(msg);
            throw new BuildException(msg);
        }
        return value;
    }


    private String getProperty(String key, String defaultValue) {
        final String value = config.getProperty(key);
        if (value == null) {
            logMessage('"' + key + "\" property not found", Project.MSG_ERR);
            return defaultValue;
        }
        return value;
    }


    private String toDir(final String conf) {
        String toDir = null;
        if (conf.equalsIgnoreCase("work")) {
            toDir = path;
        } else if (getProperty(conf + ".status", "").equalsIgnoreCase("work")) {
            toDir = getProperty(conf + ".put", null);
        }
        if (toDir != null) {
            toDir += DIR_PROGRAM;
        }
        return toDir;
    }

    
    private boolean copy182(String fromDir, String toDir) {
        return (!xpp182 || copyFile(fromDir, toDir));
    }

    
    private boolean copy190(String fromDir, String toDir) {
        return (!xpp190 || copyFile(fromDir + DIR_XPP190, toDir + DIR_XPP190));
    }


    
    private boolean copyFile(String fromDir, String toDir) {
        try {
            final File sourceFile = new File(fromDir, file);
            logMessage("Copy file " + sourceFile.getAbsolutePath() + " to " + toDir, Project.MSG_VERBOSE);
            if (!sourceFile.exists()) {
                logMessage("File not found " + sourceFile.getAbsolutePath(), Project.MSG_ERR);
                return true;
            }

            final File targetFile = new File(toDir, file);
            if (targetFile.exists() && sourceFile.lastModified() == targetFile.lastModified()) {
                logMessage(targetFile.getAbsolutePath()+ " omitted as it is up to date", Project.MSG_INFO);
                return true;
            }

            final InputStream in = new FileInputStream(sourceFile);
            final ReadableByteChannel rbc = Channels.newChannel(in);
            final FileOutputStream fos = new FileOutputStream(targetFile);
            final long transferred = fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            in.close();

            if (sourceFile.length() != transferred) {
                logMessage("Only " + transferred + " bytes transferred", Project.MSG_ERR);
                return false;
            }
            Files.setLastModifiedTime(targetFile.toPath(), FileTime.fromMillis(sourceFile.lastModified()));

            logMessage("File " + sourceFile.getAbsolutePath() + " copied to " + targetFile.getAbsolutePath(), Project.MSG_INFO);
            return true;
        } catch (IOException e) {
            logMessage(e.getMessage(), Project.MSG_ERR);
            return false;
        }
    }

    
    public void logMessage(String msg, int level) {
        if (level <= Project.MSG_ERR) {
            logProperty(msg);
        }
        super.log(msg, level);
//        System.out.println(msg);
    }

    
    private void logProperty(String msg) {
        if (errorProperty != null) {
            errorLog += (msg + "\r\n");
        }
    }

}
