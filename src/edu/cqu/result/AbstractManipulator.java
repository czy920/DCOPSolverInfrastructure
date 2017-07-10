package edu.cqu.result;

import edu.cqu.result.interfaces.Manipulator;

/**
 * Created by dyc on 2017/5/31.
 */
public abstract class AbstractManipulator implements Manipulator{
    public Object calculate(Object var1,Object var2){
        if (var1 instanceof Integer && var2 instanceof Integer){
            return calculate((int) var1,(int) var2);
        }
        else if (var1 instanceof Float && var2 instanceof Float){
            return calculate((float) var1,(float) var2);
        }
        else if (var1 instanceof Double && var2 instanceof Double){
            return calculate((double)var1,(double)var2);
        }
        else if (var1 instanceof Long && var2 instanceof Long){
            return calculate((long)var1,(long)var2);
        }
        else {
            throw new IllegalArgumentException("inputs must be numerical values!");
        }
    }
}
