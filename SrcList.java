import org.apache.tools.ant.types.resources.Files;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;

import java.util.Iterator;
import java.util.HashMap;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class SrcList extends Files {

   private String from;
   private String files;
   private String to;
   private String ifProperty;
   private String output;
   private boolean append = true;
   private boolean ignoreMissing = false;
   private boolean isDone = false;


   public SrcList() {
      super();
      setCaseSensitive(false);
   }
   

   public void setFrom(String from) {
      this.from = from;
   }

   public void setTo(String to) {
      this.to = to;
   }

   public void setOutput(String file) {
      this.output = file;
   }

   public void setAppend(boolean append) {
      this.append = append;
   }

   public void setIgnoremissing(boolean ignoreMissing) {
      this.ignoreMissing = ignoreMissing;
   }

   public void setIf(String ifProperty) {
      this.ifProperty = ifProperty;
   }

   public void setFiles(String files) {
      this.files = files;
   }


   public synchronized Iterator iterator() {
      setIncludeExclude();
      return super.iterator();
   }


   public synchronized int size() {
      setIncludeExclude();
      return super.size();
   }


   private void setIncludeExclude() {
      if (!isDone) {
         isDone = true;
         if (compareProperty()) {
            if (from==null && files==null)
               throw new BuildException("\"from\" or \"files\" attribute missed");
            if (from!=null)
               appendIncludes(getFiles(from));
            if (files!=null)
               appendIncludes(convertFiles(files.split(" ")));
         }
      }
   }


   private boolean compareProperty() {
      return ifProperty == null || getProject().getProperty(ifProperty) != null;
   }


   @SuppressWarnings("unchecked")
   public String[] getFiles(String name) {
      HashMap<String, String[]> map = (HashMap<String, String[]>)getProject().getReference("PRGLISTMAP");
      if (map==null)
         throw new BuildException("Prglist task not executed (PRGLISTMAP reference not found)");

      String[] list = map.get(name);
      if (list==null)
         throw new BuildException("Filekit \""+name+"\" not defined");

      return convertFiles(list);
   }


    private String[] convertFiles(String[] list) {
        FileWriter writer = null;
        try {
            Project prj = getProject();
            if (output !=null) {
                writer = new FileWriter(prj.resolveFile(output), append);
            }
            String[] files = new String[list.length];
            for (int i=0; i<list.length; i++) {
                String filename = convert(list[i]);
                File f = prj.resolveFile(filename);
                if (!f.exists()) {
                    log("File not found: "+files[i], Project.MSG_DEBUG);
                    if (!ignoreMissing) {
                        throw new BuildException("File not found: "+files[i]);
                    }
                } else {
                    files[i] = f.isFile() ? f.getPath() : filename;
                    if (writer != null) {
                        writer.write(files[i] + "\r\n");
                    }
                    log("append file "+files[i], Project.MSG_DEBUG);
                }
            }
            if (writer != null) {
                writer.close();
            }
            return files;
        } catch (IOException e) {
            throw new BuildException(e);
        }
    }

   private String convert(String path) {
      if (to==null)
         return path;
      else {
         StringBuilder b = new StringBuilder(to);
         int pos;
         if ((pos=b.indexOf("!\\"))>=0)
            b.replace(pos, pos+2, path.substring(0, path.lastIndexOf(File.separatorChar)));
         if ((pos=b.indexOf(".!"))>=0)
            b.replace(pos, pos+2, path.substring(path.lastIndexOf('.')));
         if ((pos=b.indexOf("!"))>=0) {
            String fname = path.substring(path.lastIndexOf(File.separatorChar)+1);
            int posDot = fname.lastIndexOf('.');
            if (posDot>=0)
               fname = fname.substring(0, posDot);
            b.replace(pos, pos+1, fname);
         }
         return b.toString();
      }
   }
}
