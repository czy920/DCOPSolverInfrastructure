package edu.cqu.algorithms.dcop.incomplete.maxsum;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LocalFunction {
    int[][] data;
    int rowId;
    int colId;
    Map<Integer,double[]> preferences;

    public LocalFunction(int[][] data, int rowId, int colId) {
        this.data = data;
        this.rowId = rowId;
        this.colId = colId;
        preferences = new HashMap<>();
        double[] rowZeroPreference = new double[data.length];
        double[] colZeroPreference = new double[data[0].length];
        Arrays.fill(rowZeroPreference,0);
        Arrays.fill(colZeroPreference,0);
        setPreferences(rowId,rowZeroPreference);
        setPreferences(colId,colZeroPreference);
    }

    public void setPreferences(int id,double[] preference){
        preferences.put(id,preference);
    }

    public double[] min(int argId,double[] utility){
        double[] result;
        if (argId == rowId){
            if (utility == null){
                utility = new double[data.length];
            }
            result = new double[data[0].length];
            for (int i = 0; i < result.length; i++){
                double minCost = Double.MAX_VALUE;
                for (int j = 0; j < data.length; j++){
                    double cost = data[j][i] + utility[j] + preferences.get(argId)[j];
                    if (minCost > cost){
                        minCost = cost;
                    }
                }
                result[i] = minCost;
            }
        }
        else {
            if (utility == null){
                utility = new double[data[0].length];
            }
            result = new double[data.length];
            for (int i = 0; i < result.length; i++){
                double minCost = Integer.MAX_VALUE;
                for (int j = 0; j < utility.length; j++){
                    double cost = data[i][j] + utility[j] + preferences.get(argId)[j];
                    if (minCost > cost){
                        minCost = cost;
                    }
                }
                result[i] = minCost;
            }
        }
        return result;
    }

    public double[] restrict(int argId,int assignment){
        double[] result;
        if (argId == rowId){
            result = new double[data[0].length];
            for (int i = 0; i < result.length; i++){
                result[i] = data[assignment][i] + preferences.get(argId)[assignment];
            }
        }
        else {
            result = new double[data.length];
            for (int i = 0; i < result.length; i++){
                result[i] = data[i][assignment] + preferences.get(argId)[assignment];
            }
        }
        return result;
    }
}
