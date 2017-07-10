package edu.cqu.parser;

import edu.cqu.core.AgentDescriptor;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dyc on 2017/6/26.
 */
public class AgentParser {
    private String agentsPath;

    public AgentParser(String agentsPath) {
        this.agentsPath = agentsPath;
    }

    public Map<String,AgentDescriptor> parse(){
        Map<String,AgentDescriptor> map = new HashMap<>();
        try {
            Element root = new SAXBuilder().build(new File(agentsPath)).getRootElement();
            List<Element> agentList = root.getChildren("agent");
            for (Element agentElement : agentList){
                String name = agentElement.getAttributeValue("name").toUpperCase();
                AgentDescriptor agentDescriptor = new AgentDescriptor();
                agentDescriptor.className = agentElement.getAttributeValue("class");
                agentDescriptor.method = agentElement.getAttributeValue("method").toUpperCase();
                map.put(name.toUpperCase(),agentDescriptor);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}
