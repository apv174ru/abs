import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;

import java.util.*;
import java.io.File;


public class BuildTask extends Task {

   private Ant antTask;
   private String target;
   private String buildfile;
   private Project project;

   public void setBuildfile(String file) {
      File f = new File(getProject().getProperty("ant.file"));
      buildfile = f.getParent()+File.separator+file;
   }


   public void setTarget(String target) {
      this.target = target;
   }


   @SuppressWarnings("unchecked")
   public void execute() throws BuildException {

      project = getProject();
      antTask = createAntTask();

      Object reference = project.getReference("rmk-properties");
      ArrayList<Hashtable<String,String>> builds = (ArrayList<Hashtable<String,String>>)reference;

      for (Hashtable<String,String> properties : builds) {
         setProperties(properties);
         evalPlatforms();
      }
   }


   private Ant createAntTask() {
      ensureValidAttributes();
      Ant ant = new Ant(this);
      ant.setAntfile(buildfile);
      ant.setTarget(target);
      return ant;
   }


   private void ensureValidAttributes() {
      assertAttributeNotNull(target, "target");
      assertAttributeNotNull(buildfile, "buildfile");
   }

   
   private void assertAttributeNotNull(Object attribute, String name) {
      if (attribute == null)
         throw new BuildException(name+" attribute missed");
   }


   private void evalPlatforms() {
      String[] platforms = project.getProperty("XBASE").split("\\W+");
      for (String platform : platforms) {
         project.setProperty("platform", platform);
         antTask.execute();
      }
   }


   private void setProperties(Hashtable<String,String> properties) {
      for (Enumeration<String> e = properties.keys(); e.hasMoreElements();) {
         String key = e.nextElement();
         String value = properties.get(key);
            project.setProperty(key, value);
      }
   }

}
