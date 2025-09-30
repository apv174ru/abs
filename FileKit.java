import org.apache.tools.ant.ProjectComponent;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;


public class FileKit extends ProjectComponent {

   private ArrayList<String> files = new ArrayList<String>();
   private String extension;
   private String srcdir;
   private String wildcards;
   private String dir;
   private String[] path;
   private DirectoryScanner ds;
   private boolean include = true;

   public FileKit() {
      super();
      ds = new DirectoryScanner();
      ds.setCaseSensitive(false);
   }

   public void setPattern(String pattern) {
      wildcards = replaceProperties(pattern);
   }

   public void setName(String name) {
      setDescription(name);
   }

   public void setExtension(String extension) {
      this.extension = "."+extension;
   }

   public void setDir(String dir) {
      this.dir = dir+File.separator;
   }

   public void setPath(String path) {
      this.path = path.split(";");
   }


   
   public String[] getFiles(String srcdir) {
      if (extension == null)
         throw new BuildException("\"extension\" attribute missed");
      this.srcdir = srcdir;
      ParseWildcards(wildcards);
      return files.toArray(new String[0]);
   }


   private void ParseWildcards(String wildcards) {
      StringTokenizer st = new StringTokenizer(wildcards, " \t\n\r\f,;");
      while (st.hasMoreElements()) {
         String pattern = st.nextToken();
         boolean saveInclude = include;
         if (pattern.charAt(0) == '-') {
            include = false;
            pattern = pattern.substring(1);
         }
         ParsePattern(pattern);
         include = saveInclude;
      }
   }


   private void ParsePattern(String pattern) {
      switch (pattern.charAt(0)) {
         case '@':
            readFromFile(pattern.substring(1));
            break;
         case '\\':
            addPattern(applyPattern(srcdir+pattern.substring(1)));
            break;
         default: 
            if (!include && pattern.equals("*"))
               files.clear();
            else
               addPattern(applyPatternInPath(pattern));
      }
   }


   private void addPattern(List<String> filenames) {
      for (String file : filenames) {
         file = file.toLowerCase();
         if (!include) {
            files.remove(file);
            getProject().log(getDescription()+": exclude file "+file, Project.MSG_VERBOSE);
         } else if (!files.contains(file)) {
            files.add(file);
            getProject().log(getDescription()+": include file "+file, Project.MSG_VERBOSE);
         }
      }
   }


   private List<String> applyPattern(String pattern) {
      pattern = addExtension(pattern);
      ds.setIncludes(new String[] {pattern});
      ds.scan();
      return Arrays.asList(ds.getIncludedFiles());
   }


   private List<String> applyPatternInPath(String pattern) {
      if (pattern.startsWith("..\\xlib\\")) {
         Project project = getProject();
         pattern = project.getProperty("build-path")+pattern.substring(7);
         pattern = project.resolveFile(pattern).getPath();
      } else if (pattern.startsWith("..\\"))
         pattern = getProject().resolveFile(pattern).getPath();
      else if (pattern.startsWith("\\\\") || pattern.indexOf(':') != -1)
         ; // полный путь
      else if (dir != null)
         pattern = dir+pattern;
      else {
         List<String> list = new ArrayList<String>();
         for (String p : path)
            list.addAll(applyPattern(p+File.separator+pattern));
         return list;
      }
      return applyPattern(pattern);
   }


   private String addExtension(String pattern) {
      int skip = pattern.lastIndexOf(File.separatorChar);
      if (skip < 0)
         skip = 0;
      int pos = pattern.lastIndexOf('.');
      if (pos < skip)
         pattern += extension;
      else if (!pattern.substring(pos).equalsIgnoreCase(extension))
         pattern = pattern.substring(0, pos)+extension;
      return pattern;
   }
   
   
   private void readFromFile(String filename) throws BuildException {
      try {
         File file;
         if (filename.startsWith("\\")) {
            file = new File(srcdir+filename);
         } else {
            file = new File(dir+filename);
            if (!file.exists())
               file = new File(getProject().getBaseDir()+File.separator+filename);
         }

         BufferedReader reader = new BufferedReader(new FileReader(file));
         String s;
         while ((s=reader.readLine())!=null)
            ParseWildcards(s);
         reader.close();
      } catch (IOException e) {
         throw new BuildException(e);
      }
   }


   private String replaceProperties(String s) {
      int posB;
      int posE = -1;

      Project project = getProject();
      while ((posB=s.indexOf("$(", posE+1)) >= 0 && (posE=s.indexOf(')', posB+2)) > 0) {
         String key = s.substring(posB+2, posE);
         String value = project.getProperty(key);
         if (value == null)
            value = project.getProperty("env."+key);
         if (value != null)
            s = s.substring(0, posB)+value+s.substring(posE+1);
      }
      return s;
   }

}

