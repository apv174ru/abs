import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.resources.FileResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;


public class PrgListTask extends Task {

   private ArrayList<FileKit> filekits = new ArrayList<FileKit>();
   private String srcdir;


   public void addFileKit(FileKit filekit) {
      filekits.add(filekit);
   }

   public void setSrcdir(String srcdir) {
      this.srcdir = srcdir;
      if (!srcdir.endsWith(File.separator)) 
        this.srcdir += File.separator;
   }


   public void execute() throws BuildException {
      if (srcdir==null)
         throw new BuildException("\"srcdir\" attribute missed");

      HashMap<String, String[]> map = new HashMap<String, String[]>();
      for (FileKit fk : filekits) 
         map.put(fk.getDescription(), fk.getFiles(srcdir));
      getProject().addReference("PRGLISTMAP", map);
   }

}
