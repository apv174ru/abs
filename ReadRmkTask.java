import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.io.*;
import java.util.Hashtable;
import java.util.ArrayList;


public class ReadRmkTask extends Task {

    private File baseDir;
    private Project project;
    private boolean addNewProjectFlag = true;
    private ArrayList<Hashtable<String,String>> properties = new ArrayList<Hashtable<String,String>>();
    private String propertyFile;
    private File rmk;

    public void setFile(String file) {
        propertyFile = file;
    }
   
   
    public void execute() throws BuildException {
      
        initExecute();

        if (propertyFile.equalsIgnoreCase("none")) {
            return;
        } else if (propertyFile.equalsIgnoreCase("true")) {
            setDefaultRmk(baseDir.getName(), "build");
        } else if (propertyFile.equalsIgnoreCase("false")) {
            setDefaultRmk("build", baseDir.getName());
        } else if (!propertyFile.isEmpty()) {
            rmk = project.resolveFile(propertyFile);
        }

        addNewProject();

        String addonName = "addon.rmk";
        // project.rmk
        if (rmk != null) {
            readProperties(rmk);
            addonName = rmk.getParent() + File.separator + addonName;
        }

        // addon.rmk
        File addon = new File(addonName);
        if (addon.exists()) {
            addNewProjectFlag = false;
            readProperties(addon);
        }

        project.addReference("rmk-properties", properties);
    }


    // по-умолчанию rmk с именем каталога или build.rmk
    private void setDefaultRmk(String... filenames) {
        for (final String filename : filenames) {
            if (SetRmk(filename)) {
                return;
            }
        }
        FindRmkInBaseDir();
    }

   
    private boolean SetRmk(String name) {
        rmk = new File(baseDir.getPath() + File.separator + name + ".rmk");
        return rmk.exists();
    }


    private void FindRmkInBaseDir() {
        File[] rmkFiles = baseDir.listFiles(new FilenameFilter() {
            public boolean accept(File d, String name) {
                return name.endsWith(".rmk");
            }
        });
        if (rmkFiles.length == 0) {
         throw new BuildException("Build file (.rmk) not found in " + baseDir);
        } else if (rmkFiles.length > 1) {
            throw new BuildException("Ambiguous build files (.rmk) in " + baseDir);
        }
        rmk = rmkFiles[0];
    }


    private void initExecute() {
        project = getProject();
        baseDir = project.getBaseDir();
        if (baseDir == null) {
            baseDir = new File(".");
        }
    }

   
    private void readProperties(File file) {
        try {
            log("Reading property file: "+file, Project.MSG_VERBOSE);

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(file), "cp866"));

            String s;
            while ((s = nextLine(reader)) != null) {
                if (s.startsWith("#ifdef")) {
                    ifClause(reader, s);
                } else {
                readLine(s);
                }
            }
            reader.close();
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }


    private void readLine(String s) {
        if (s.charAt(0) != '#') {
            int pos = s.indexOf(":=");
            if (pos != -1) {
                String key = s.substring(0, pos).trim().toUpperCase();
                String value = replaceProperties(s.substring(pos + 2).trim());
                putProperty(key, value);
            }
        } else if (s.startsWith("#include")) {
            if (s.toLowerCase().indexOf("common.rmk") >= 0) {
                addNewProjectFlag = true;
            } else {
                readProperties(resolveInclude(s.substring(9)));
            }
        } else {
            throw new BuildException("Unexpected command in line: " + s);
        }

    }


    private void putProperty(String key, String value) {
        addNewProject();
        int i = properties.size() - 1;
        properties.get(i).put(key, value);
    }


    private String getProperty(String key) {
        int i = properties.size() - 1;
        return properties.get(i).get(key);
    }


    private void addNewProject() {
        if (addNewProjectFlag) {
            addNewProjectFlag = false;
            properties.add(new Hashtable<String,String>());
        }
    }

   
    private void elseClause(BufferedReader reader, boolean skip) throws IOException {
        String s;
        while ((s = nextLine(reader)) != null) {
            if (s.startsWith("#endif")) {
                return;
            } else if (!skip) {
                readLine(s);
            }
        }
    }


    private void ifClause(BufferedReader reader, String s) throws IOException {
        boolean skip = (project.getProperty(s.substring(7).trim()) == null);

        while ((s = nextLine(reader)) != null) {
            if (s.startsWith("#else")) {
                elseClause(reader, !skip);
            } else if (s.startsWith("#endif")) {
                return;
            } else if (!skip) {
                readLine(s);
            }
        }
    }


    private String nextLine(BufferedReader reader) throws IOException {
        String s;
        while ((s = reader.readLine()) != null) {
            s = s.trim();
            if (!s.startsWith("//") && !s.isEmpty()) {
                break;
            }
        }
        return s;
    }


    private File resolveInclude(String s) {
        s = s.trim();
        if ((s.startsWith("\"") && s.endsWith("\""))) {
            s = s.substring(1, s.length() - 1);
        }
        return project.resolveFile(replaceProperties(s));
    }


    private String replaceProperties(String s) {
        int posB;
        int posE = -1;

        while ((posB = s.indexOf("$(", posE + 1)) >= 0 && 
               (posE = s.indexOf(')', posB + 2)) > 0) {
            String key = s.substring(posB + 2, posE);
            String value = getProperty(key);
            if (value == null) {
                value = project.getProperty(key);
            }
            if (value == null) {
                value = project.getProperty("env." + key);
            }
            if (value != null) {
                s = s.substring(0, posB) + value + s.substring(posE + 1);
            }
        }
        return s;
    }
}
