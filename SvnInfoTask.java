import java.util.StringTokenizer;
import java.io.UnsupportedEncodingException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;


public class SvnInfoTask extends Task {

   private String property;

   public void setProperty(String property) {
      this.property = property;
   }


   public void execute() throws BuildException {
      if (property == null)
          throw new BuildException("The \"property\" attribute is required.");

      Project project = getProject();
      
      String text = project.getProperty(property);
      if (text == null)
          throw new BuildException("Empty property \""+property+"\" value.");

      try {
      
         StringTokenizer st = new StringTokenizer(text, "\n\r");
         while (st.hasMoreElements()) {
            String string = st.nextToken();

            int pos = string.indexOf(':');
            if (pos >= 0) {
               String key = property+"."+string.substring(0, pos).replaceAll(" ", "");
               String value  = string.substring(pos+2);
               project.setProperty(key, new String(value.getBytes(), "cp866"));
            }
         }

      } catch (UnsupportedEncodingException e) {
         throw new BuildException(e);
      }

   }
}
