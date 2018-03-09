package edu.cqu.benchmark.randomdcops;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;

public class DCOPConvert {

    private static final int MODE_S2A = 0;
    private static final int MODE_A2S = 1;

    public static void convert(String path, String convertedPath, int mode) throws Exception{
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(convertedPath));
        BufferedReader reader = new BufferedReader(new FileReader(path));
        while (true){
            String line = reader.readLine();
            if (line == null){
                break;
            }
            if (!line.trim().startsWith("<relation ")){
                if (line.contains("type=")){
                    if (mode == MODE_A2S){
                        line = line.replace("type=\"ADCOP\"","type=\"DCOP\"");
                    }
                    else {
                        line = line.replace("type=\"DCOP\"","type=\"ADCOP\"");
                    }
                }
                writer.write(line + "\n");
            }
            else {
                int tupleStartIndex = line.indexOf('>') + 1;
                String prefix = line.substring(0,tupleStartIndex);
                int tupleEndIndex = line.lastIndexOf('<') - 1;
                String subfix = line.substring(tupleEndIndex + 1);
                String content = line.substring(tupleStartIndex,tupleEndIndex + 1);
                String[] tuples = content.split("\\|");
                content = "";
                for (String tuple : tuples){
                    String[] tokens = tuple.split(":");
                    String costToken = processToken(tokens[0], mode);
                    content += costToken + ":" + tokens[1] + "|";
                }
                content = content.substring(0, content.length() - 1);
                writer.write(prefix + content + subfix + "\n");
            }
        }
        writer.close();
    }

    private static String processToken(String costToken, int mode) {
        if (mode == MODE_A2S){
            String[] costs = costToken.split(" ");
            return String.valueOf(Integer.parseInt(costs[0]) + Integer.parseInt(costs[1]));
        }
        else {
            int totalCost = Integer.parseInt(costToken);
            int randomCost = (int) (Math.random() * totalCost);
            return randomCost + " " + (totalCost - randomCost);
        }

    }

    public static void main(String[] args) throws Exception{
        convert("problem/adcop/70/sparse/RANDOM_ADCOP_70_10_density_0.1_0.xml","problem/adcop/70/sparse/RANDOM_ADCOP_70_10_density_0.1_0_converted.xml",MODE_A2S);
    }


}
