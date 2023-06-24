package io.github.agentsoz.ees.jadexextension.masterthesis.JadexAgent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Objects;

public class XmlJavaParse {

    public static Object[] extractXMLData() {
        Object[] requiredBDIinfo = new Object[2];
        try {
            // creating a constructor of file class and
            // parsing an XML file


            File file = new File(
                    "C:\\Users\\jamie\\code\\ees - Copy\\ees\\src\\main\\java\\io\\github\\agentsoz\\ees\\jadexextension\\jadexagent\\TrikeWorld.application.xml");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            doc.getDocumentElement().normalize();

            System.out.println(
                    "Root element: "
                            + doc.getDocumentElement().getNodeName());

            NodeList nodeList = doc.getElementsByTagName("configuration");

            for (int x = 0, size = nodeList.getLength(); x < size; x++) {
                Node node = nodeList.item(x);
                Element tElement = (Element) node;
                NodeList nodeList1 = tElement.getElementsByTagName("components");
             //   System.out.println(nodeList1.getLength());
                for (int y = 0, size2 = nodeList1.getLength(); y < size2; y++) {
                    Node node1 = nodeList1.item(y);
                    Element tElement1 = (Element) node1;
                    NodeList nodeList2 = tElement.getElementsByTagName("component");
                    for (int z = 0, size3 = nodeList2.getLength(); z < size3; z++) {
                        System.out.println(nodeList2.getLength());
                        String componentname = nodeList2.item(z).getAttributes().getNamedItem("type").getNodeValue();
                        String componentnumber = nodeList2.item(z).getAttributes().getNamedItem("number").getNodeValue();
                        if (Objects.equals(componentname, "TrikeAgent"))
                        {
                            System.out.println("whoohooo");
                            requiredBDIinfo[0] = Integer.parseInt(componentnumber);
                        //    System.out.println(requiredBDIinfo[0]);
                            // TrikeAgentnumber = Integer.parseInt(componentnumber);
                        }
                        if (Objects.equals(componentname, "SimSensoryInputBroker")) {
                            requiredBDIinfo[1] = Integer.parseInt(componentnumber);
                            System.out.println(requiredBDIinfo[1]);
                            //  SimSensoryInputBrokernumber = Integer.parseInt(componentnumber);
                        }

                    }


                }
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        return requiredBDIinfo;
    }


    public static void main(String argv[])
    {extractXMLData();}
}
