import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.condition.*;
import org.apache.tools.ant.taskdefs.Available;

import java.util.ArrayList;


public class SetPropTask extends Task {

   private String name;
   private String ifValue;
   private String elseValue;
   private ArrayList<Condition> conditions = new ArrayList<Condition>();


   public void setName(String name) {
      this.name = name;
   }

   public void setValue(String value) {
      this.ifValue = value;
   }

   public void setElse(String alternative) {
      this.elseValue = alternative;
   }

   public void addCondition(Condition c) {
      conditions.add(c);
   }

   public void addEquals(Equals c) {
      addCondition(c);
   }

   public void addOr(Or c) {
      addCondition(c);
   }

   public void addAnd(And c) {
      addCondition(c);
   }

   public void addIsSet(IsSet c) {
       addCondition(c);
   }

   public void addAvailable(Available c) {
       addCondition(c);
   }

   public void addContains(Contains c) {
       addCondition(c);
   }


   public void execute() throws BuildException {
      if (name == null) {
          throw new BuildException("The \"name\" attribute is required.");
      }

      boolean set = true;
      if (conditions.size() > 1) {
          throw new BuildException("You must not nest more than one "
              + "condition into <" + getTaskName() + ">");
      } else if (conditions.size() == 1) {
         Condition c = conditions.get(0);
         set = c.eval();
      }

      if (set) {
          log("Condition true; setting " + name + " to " + ifValue, Project.MSG_DEBUG);
          setProp(name, ifValue);
      } else if (elseValue !=null) {
         log("Condition false; setting " + name + " to " + elseValue, Project.MSG_DEBUG);
         setProp(name, elseValue);
      } else {
          log("Condition false; not setting " + name, Project.MSG_DEBUG);
      }
   }

   public void setProp(String name, String value) {
      Project p = getProject();

      if ( p.getUserProperty(name) !=  null )
         p.setUserProperty(name, value);
      else
         p.setProperty(name, value);
   }

}
