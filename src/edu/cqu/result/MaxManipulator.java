package edu.cqu.result;

/**
 * Created by dyc on 2017/6/15.
 */
public class MaxManipulator extends AbstractManipulator {
    @Override
    public int calculate(int var1, int var2) {
        return Integer.max(var1,var2);
    }

    @Override
    public float calculate(float var1, float var2) {
        return Float.max(var1,var2);
    }

    @Override
    public double calculate(double var1, double var2) {
        return Double.max(var1, var2);
    }

    @Override
    public long calculate(long var1, long var2) {
        return Long.max(var1,var2);
    }
}
