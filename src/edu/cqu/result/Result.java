package edu.cqu.result;

import edu.cqu.result.annotations.AverageField;
import edu.cqu.result.annotations.CycleLength;
import edu.cqu.result.annotations.CycleOffset;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by dyc on 2017/5/29.
 */
public class Result {
    @AverageField
    protected double totalCost;
    @AverageField
    protected int messageQuantity;
    @AverageField
    protected long totalTime;
    @AverageField
    protected int ncccs;
    protected Map<Integer,Integer> agentValues;
    private int cycleLength;
    private int cycleOffset;

    public int getMessageQuantity() {
        return messageQuantity;
    }

    public Result(){
        cycleLength = cycleOffset = -1;
        if (!checkFields()){
            throw new IllegalArgumentException("multiple CycleLength or CycleOffset found!");
        }
        agentValues = new HashMap<>();
    }

    public void setNcccs(int ncccs) {
        this.ncccs = ncccs;
    }

    public int getNcccs() {
        return ncccs;
    }

    public void setMessageQuantity(int messageQuantity) {
        this.messageQuantity = messageQuantity;
    }

    public void substitute(Result result){
        if (!this.getClass().getName().equals(result.getClass().getName())){
            throw new IllegalArgumentException("results must in same class!");
        }
        try {
            Set<Field> allFields = new HashSet<>();
            getAllFields(result,allFields);
            for (Field field : allFields) {
                Object resultValue = field.get(result);
                field.setAccessible(true);
                if (field.getType().isPrimitive()){
                    field.set(this,resultValue);
                }
                else if (field.getType().isArray()){
                    int arrayLength = Array.getLength(resultValue);
                    Object array =  Array.newInstance(field.getType().getComponentType(),arrayLength);
                    for (int i = 0; i < arrayLength; i++){
                        Array.set(array,i,Array.get(resultValue,i));
                    }
                    field.set(this,array);
                }
                else if (resultValue instanceof Collection){
                    Collection resultCollection = (Collection) resultValue;
                    Collection collection = (Collection) resultValue.getClass().newInstance();
                    collection.addAll(resultCollection);
                    field.set(this,collection);
                }
                else if (resultValue instanceof Map){
                    Map resultMap = (Map) resultValue;
                    Map map = (Map) resultValue.getClass().newInstance();
                    for (Object key : resultMap.keySet()){
                        map.put(key,resultMap.get(key));
                    }
                    field.set(this,map);
                }
            }
        } catch (IllegalAccessException e){
            System.out.println("substitute failed, reason:" + e.getStackTrace());
        } catch (InstantiationException e) {
            System.out.println("substitute failed, reason:" + e.getStackTrace());
        }
    }

    public boolean checkFields(){
        boolean flag = true;
        int cycleLengthFound = 0;
        int cycleOffsetFound = 0;
        try {
            Set<Field> allFields = new HashSet<>();
            getAllFields(this,allFields);
            for (Field field : allFields) {
                if (field.isAnnotationPresent(CycleLength.class)) {
                    cycleLength = field.getInt(this);
                    cycleLengthFound++;
                } else if (field.isAnnotationPresent(CycleOffset.class)) {
                    cycleOffset = field.getInt(this);
                    cycleOffsetFound++;
                }
                if (cycleLengthFound > 1 || cycleOffsetFound > 1) {
                    flag = false;
                    break;
                }
            }
        } catch (IllegalAccessException e){
            flag = false;
            System.out.println("retrieve cycle length or cycle offset failed!");
        }
        return flag;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public void setTotalTime(long totalTime) {
        this.totalTime = totalTime;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public long getTotalTime() {
        return totalTime;
    }

    public Set<Integer> getAgents(){
        return agentValues.keySet();
    }

    public int getAgentValue(int agentId){
        return agentValues.get(agentId);
    }

    public void setAgentValues(int agentId,int agentValue){
        agentValues.put(agentId,agentValue);
    }

    public void max(Result result){
        arithmeticManipulate(result,new MaxManipulator());
    }

    public void min(Result result){
        arithmeticManipulate(result,new MinManipulator());
    }

    public void add(Result result){
        arithmeticManipulate(result,new AddManipulator());
    }

    public void subtract(Result result){
        arithmeticManipulate(result,new SubtractManipulator());
    }

    public void average(int times){
        Set<Field> allFields = new HashSet<>();
        getAllFields(this,allFields);
        try {
            for (Field field : allFields) {
                Object value = field.get(this);
                if (!field.isAnnotationPresent(AverageField.class)) {
                    continue;
                }
                if (field.getType().isPrimitive()) {
                    field.set(this, div(value, times));
                }
                else if (field.getType().isArray()){
                    Object array = value;
                    int length = Array.getLength(array);
                    for (int i = 0; i < length; i++){
                        Object val = Array.get(array,i);
                        Array.set(array,i,div(val,times));
                    }
                }
                else if (value instanceof List){
                    List list = (List) value;
                    for (int i = 0; i < list.size(); i++){
                        Object old = list.get(i);
                        list.set(i,div(old,times));
                    }
                }
            }
        } catch (Exception e){

        }
    }

    private Object div(Object num, int den){
        if (num instanceof Integer){
            return (int)num / den;
        }
        if (num instanceof Float){
            return (float)num / den;
        }
        if (num instanceof Double){
            return (double)num / den;
        }
        if (num instanceof Long){
            return (long)num / den;
        }
        throw new IllegalArgumentException("Unknown num type");
    }

    private void arithmeticManipulate(Result result,AbstractManipulator manipulator){
        if (!this.getClass().getName().equals(result.getClass().getName())){
            throw new IllegalArgumentException("results must in same class!");
        }
        try {
            Set<Field> allFields = new HashSet<>();
            getAllFields(result,allFields);
            for (Field field : allFields) {
                if (field.isAnnotationPresent(AverageField.class)) {
                    Object resultValue = field.get(result);
                    if (field.getType().isPrimitive()) {
                        field.set(this,manipulator.calculate(resultValue,field.get(this)));
                    } else if (field.getType().isArray()) {
                        int resultLength = Array.getLength(resultValue);
                        Object myArray =  field.get(this);
                        int myLength = Array.getLength(myArray);
                        Object calculatedArray = null;
                        if (cycleLength == -1 && result.cycleLength == -1) {
                            int myOffset = cycleOffset == -1 ? 0 : cycleOffset;
                            int resultOffset = result.cycleOffset == -1 ? 0 : result.cycleOffset;
                            calculatedArray = Array.newInstance(field.getType().getComponentType(),Integer.max(resultLength,myLength) - Integer.min(myOffset,resultOffset));
                            for (int i = Integer.min(myOffset,resultOffset); i < Integer.max(resultLength,myLength); i++) {
                                Object rv = null,mv = null,rs = null;
                                if (i < resultLength){
                                    if (i < resultOffset){
                                        rv = Array.get(resultValue,resultOffset);
                                    }
                                    else {
                                        rv = Array.get(resultValue, i);
                                    }
                                    rs = rv;
                                }
                                if (i < myLength){
                                    if (i < myOffset){
                                        mv = Array.get(myArray,myOffset);
                                    }
                                    else {
                                        mv = Array.get(myArray,i);
                                    }
                                    rs = mv;
                                }
                                if (rv != null && mv != null){
                                    rs = manipulator.calculate(rv,mv);
                                }
                                Array.set(calculatedArray, i, rs);
                            }
                        }
                        else if (Integer.min(cycleLength,result.cycleLength) < 0){
                            throw new IllegalArgumentException("illegal cycle length!");
                        }
                        else {
                            throw new UnsupportedOperationException("");
                        }
                        field.set(this,calculatedArray);
                    }
                    else if (resultValue instanceof List){
                        List myList = (List) field.get(this);
                        List otherList = (List) resultValue;
                        List calculatedList = null;
                        try {
                             calculatedList = (List) field.getType().newInstance();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        int resultLength = otherList.size();
                        int myLength = myList.size();
                        if (cycleLength == -1 && result.cycleLength == -1){
                            int myOffset = cycleOffset == -1 ? 0 : cycleOffset;
                            int resultOffset = result.cycleOffset == -1 ? 0 : result.cycleOffset;
                            for (int i = Integer.min(myOffset,resultOffset); i < Integer.max(resultLength,myLength); i++) {
                                Object rv = null,mv = null,rs = null;
                                if (i < resultLength){
                                    if (i < resultOffset){
                                        rv = otherList.get(resultOffset);
                                    }
                                    else {
                                        rv = otherList.get(i);
                                    }
                                    rs = rv;
                                }
                                if (i < myLength){
                                    if (i < myOffset){
                                        mv = myList.get(myOffset);
                                    }
                                    else {
                                        mv = myList.get(i);
                                    }
                                    rs = mv;
                                }
                                if (rv != null && mv != null){
                                    rs = manipulator.calculate(rv,mv);
                                }
                                calculatedList.add(rs);
                            }
                        }
                        else if (Integer.min(cycleLength,result.cycleLength) < 0){
                            throw new IllegalArgumentException("illegal cycle length!");
                        }
                        else {
                            throw new UnsupportedOperationException("");
                        }
                        field.set(this,calculatedList);
                    }
                    else if (resultValue instanceof Map){
                        Map calculatedMap = new HashMap();
                        Map otherMap = (Map) resultValue;
                        Map myMap = (Map) field.get(this);
                        Set<Object> allKeys = myMap.keySet();
                        allKeys.addAll(otherMap.keySet());
                        for (Object key : allKeys){
                            if (myMap.containsKey(key) && otherMap.containsKey(key)){
                                calculatedMap.put(key,manipulator.calculate(myMap.get(key),otherMap.get(key)));
                            }
                            else if (myMap.containsKey(key)){
                                calculatedMap.put(key,myMap.get(key));
                            }
                            else if (otherMap.containsKey(key)) {
                                calculatedMap.put(key,otherMap.get(key));
                            }
                        }
                        field.set(this,calculatedMap);
                    }
                }
            }
        } catch (IllegalAccessException e){
            e.printStackTrace();
        }
    }

    private void getAllFields(Result result,Set<Field> fields){
        Class clazz = result.getClass();
        while (clazz != null){
            for (Field field : clazz.getDeclaredFields()){
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
    }
}
