package edu.cqu.result;

/**
 * Created by dyc on 2017/6/15.
 */
public class SubtractManipulator extends AbstractManipulator {

    @Override
    public int calculate(int var1, int var2) {
        return var1 - var2;
    }

    @Override
    public float calculate(float var1, float var2) {
        return var1 - var2;
    }

    @Override
    public double calculate(double var1, double var2) {
        return var1 - var2;
    }

    @Override
    public long calculate(long var1, long var2) {
        return var1 - var2;
    }
}
