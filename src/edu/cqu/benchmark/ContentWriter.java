package edu.cqu.benchmark;

import edu.cqu.benchmark.graphcoloring.GraphColoringGenerator;
import edu.cqu.benchmark.graphcoloring.WeightedGraphColoringGenerator;
import edu.cqu.benchmark.randomdcops.RandomDCOPGenerator;
import edu.cqu.benchmark.scalefreenetworks.ScaleFreeNetworkGenerator;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by YanChenDeng on 2016/4/18.
 */
public class ContentWriter {

    public static final String PROBLEM_SCALE_FREE_NETWORK = "scalefreenetwork";
    public static final String PROBLEM_RANDOM_DCOP = "RANDOMDCOP";
    public static final String PROBLEM_GRAPH_COLORING = "GRAPH_COLORING";
    public static final String PROBLEM_WEIGHTED_GRAPH_COLORING = "WEIGHTED_GRAPH_COLORING";

    private int nbInstance;
    private String dirPath;
    private int nbAgent;
    private int domainSize;
    private int minCost;
    private int maxCost;
    private String problemType;
    private Map<String,Object> extraParameter;

    public ContentWriter(int nbInstance, String dirPath, int nbAgent, int domainSize, int minCost, int maxCost, String problemType,Map<String,Object> extraParameter) {
        this.nbInstance = nbInstance;
        this.dirPath = dirPath;
        this.nbAgent = nbAgent;
        this.domainSize = domainSize;
        this.minCost = minCost;
        this.maxCost = maxCost;
        this.problemType = problemType;
        this.extraParameter = extraParameter;
    }

    public void setExtraParameter(Map<String, Object> extraParameter) {
        this.extraParameter = extraParameter;
    }

    public void generate() throws Exception{
        Format format = Format.getPrettyFormat();
        format.setEncoding("UTF-8");
        XMLOutputter outputter = new XMLOutputter(format);
        int base = 0;
        File f=new File(dirPath);
		if(!f.exists())
		{
			f.mkdirs();
		}
        String filenameBase =dirPath + "\\" + problemType + "_" + nbAgent + "_" + domainSize + "_";
        for (String key : extraParameter.keySet()){
            filenameBase += key + "_";
            filenameBase += extraParameter.get(key) + "_";
        }
        while (true){
            String fileName = filenameBase + base + ".xml";
            if (!new File(fileName).exists())
                break;
            base++;
        }
        for (int i = 0; i < nbInstance; i++){
            FileOutputStream stream = new FileOutputStream(filenameBase+ (base + i) + ".xml");
            Element root = new Element("instance");
            AbstractGraph graph = null;
            if (problemType.equals(PROBLEM_SCALE_FREE_NETWORK)) {
                graph = new ScaleFreeNetworkGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(Integer)extraParameter.get("m1"),(Integer)extraParameter.get("m2"));
            }
            else if (problemType.equals(PROBLEM_RANDOM_DCOP)){
                graph = new RandomDCOPGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"));
            } else if (problemType.equals(PROBLEM_GRAPH_COLORING)) {
                graph = new GraphColoringGenerator("instance" + i,nbAgent,domainSize,(double)extraParameter.get("density"));
            }
            else if (problemType.equals(PROBLEM_WEIGHTED_GRAPH_COLORING)){
                graph = new WeightedGraphColoringGenerator("instance" + i,nbAgent,domainSize,minCost,maxCost,(double)extraParameter.get("density"));
            }
            graph.generateConstraint();
            root.addContent(graph.getPresentation());
            root.addContent(graph.getAgents());
            root.addContent(graph.getDomains());
            root.addContent(graph.getVariables());
            root.addContent(graph.getConstraints());
            root.addContent(graph.getRelations());
            root.addContent(graph.getGuiPresentation());
            outputter.output(root,stream);
            stream.close();
        }
    }

    public static void main(String[] args) throws Exception{
        Map<String,Object> para = new HashMap<String, Object>();
        para.put("density",0.01);
        ContentWriter writer = new ContentWriter(1,"C:\\Users\\admin\\Desktop\\test0518",80,10,1,100,PROBLEM_RANDOM_DCOP,para);
        writer.generate();
    }
}
