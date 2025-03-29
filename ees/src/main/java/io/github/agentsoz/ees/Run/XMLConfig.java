package io.github.agentsoz.ees.Run;

/*-
 * #%L
 * Emergency Evacuation Simulator
 * %%
 * Copyright (C) 2014 - 2025 by its authors. See AUTHORS file.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import io.github.agentsoz.ees.util.Parser;
import org.w3c.dom.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;

public class XMLConfig {

    public void applyConfig(Element configRoot) {
        NodeList targetFileNL = configRoot.getElementsByTagName("file");

        //  iterate through each file tag, set params and write back to file
        for (int i = 0; i < targetFileNL.getLength(); i++) {
            Element targetFileEl = (Element) targetFileNL.item(i);
            String targetFileName = targetFileEl.getAttribute("target_file");

            //  target file
            Element targetFileRoot = Parser.parseXML(targetFileName);

            //  set param tags from custom config to target
            setParams(targetFileEl, targetFileRoot);

            //  update target file with new params
            writeBack(targetFileRoot, targetFileName, targetFileRoot.getOwnerDocument().getDoctype());
        }
    }

    private void setParams(Element sourceRoot, Element targetRoot) {
        //  param tags
        NodeList childNL = sourceRoot.getChildNodes();

        for (int i = 0; i < childNL.getLength(); i++) {
            Node sourceNode = childNL.item(i);
            if (sourceNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element sourceElement = (Element) sourceNode;

            String[] targetPath = sourceElement.getAttribute("target_path").split("/");
            Element targetElement = targetRoot;

            //traverse
            for (String subPath : targetPath) {
                if (targetElement == null) {
                    break;
                }

                String tagName = subPath;

                String attrs;
                String[] attrArr = new String[0];;

                int itemNum = 0;

                int arrowIndex = subPath.indexOf("->");
                int numIndex = subPath.indexOf("#");

                //  sets attributes if exists
                if (arrowIndex != -1) {
                    tagName = tagName.substring(0, arrowIndex);
                    attrs = subPath.substring(arrowIndex + 2);
                    attrArr = attrs.split(";");

                    if (attrArr.length == 0) {
                        attrArr = new String[1];
                        attrArr[0] = attrs;
                    }
                }

                // item number given
                if (numIndex != -1) {
                    tagName = tagName.substring(numIndex + 1);
                    NodeList subPathNL = targetElement.getElementsByTagName(tagName);
                    itemNum = Integer.parseInt(subPath.substring(0, numIndex));

                    if (subPathNL.getLength() == 0 || subPathNL.getLength() < itemNum) {
                        targetElement = null;
                        break;
                    }

                    if (attrArr.length == 0) {
                        Node node = subPathNL.item(itemNum - 1);
                        targetElement = lookAttrs(attrArr, node);
                    } else {
                        int counter = 0;
                        for (int j = 0; j < subPathNL.getLength(); j++) {
                            Node node = subPathNL.item(j);
                            targetElement = lookAttrs(attrArr, node);

                            if (targetElement != null && ++counter == itemNum) {
                                break;
                            }
                        }
                        if (counter != itemNum) {
                            targetElement = null;
                        }
                    }
                } //  no item number given, look for the first match
                else {
                    NodeList subPathNL = targetElement.getElementsByTagName(tagName);

                    if (subPathNL.getLength() == 0) {
                        targetElement = null;
                        break;
                    }

                    for (int j = 0; j < subPathNL.getLength(); j++) {
                        Node node = subPathNL.item(j);
                        targetElement = lookAttrs(attrArr, node);

                        if (targetElement != null) {
                            break;
                        }
                    }
                }
            }

            //no target element found during traverse
            if (targetElement == null) {
                System.out.println(Arrays.toString(targetPath) + " is a wrong path!!!");
                break;
            }

            //  import source tree to target dom
            Element importedSourceElement = (Element) targetElement.getOwnerDocument().importNode(sourceElement, true);
            //remove config attrs
            importedSourceElement.removeAttribute("target_path");
            importedSourceElement.removeAttribute("config_mode");

            //replace trees
            if (sourceElement.getAttribute("config_mode").equals("replace")) {
                targetElement.getParentNode().replaceChild(importedSourceElement, targetElement);
            } else {
                //store existing attributes
                NamedNodeMap originAttrs = targetElement.getAttributes();
                //replace trees
                targetElement.getParentNode().replaceChild(importedSourceElement, targetElement);
                //put existing attributes back
                for (int j = 0; j < originAttrs.getLength(); j++) {
                    String attrName = originAttrs.item(j).getNodeName();
                    String attrValue = originAttrs.item(j).getNodeValue();

                    //  if already exists in new element, then skip
                    if (importedSourceElement.hasAttribute(attrName)) {
                        continue;
                    }
                    //  set attribute
                    importedSourceElement.setAttribute(attrName, attrValue);
                }
            }
        }
    }

    public String[] setArgs(Element configRoot) {
        String[] args = new String[2];
        args[0] = "--config";
        args[1] = configRoot.getElementsByTagName("config").item(0).getTextContent();
        return args;
    }

    public String[] setArgs(Element configRoot) {
        String[] args = new String[2];
        args[0] = "--config";
        args[1] = configRoot.getElementsByTagName("config").item(0).getTextContent();
        return args;
    }

    /**
     * checks if node satisfies all attributes, converts the node to element and
     * returns back
     *
     */
    private Element lookAttrs(String[] attrArr, Node node) {
        for (String s : attrArr) {
            String[] parts = s.split("=");

            String attrName = parts[0];
            String attrValue = parts[1];

            Node attrNode = node.getAttributes().getNamedItem(attrName);

            if (attrNode == null) {
                return null;
            }

            if (!attrNode.getTextContent().equals(attrValue)) {
                return null;
            }
        }
        return (Element) node;
    }

    /**
     * modifies the config file
     *
     */
    private void writeBack(Element targetFileRoot, String path, DocumentType doctype) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = transformerFactory.newTransformer();

            // Set the doctype if provided
            if (doctype != null) {
                String publicId = doctype.getPublicId();
                String systemId = doctype.getSystemId();
                if (publicId != null) {
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, publicId);
                }
                if (systemId != null) {
                    transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, systemId);
                }
            }
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        DOMSource source = new DOMSource(targetFileRoot.getOwnerDocument());

        try {
            StreamResult result = new StreamResult(new FileOutputStream(path));
            transformer.transform(source, result);
            if (result.getOutputStream() != null) {
                result.getOutputStream().flush();
                result.getOutputStream().close();
            } else if (result.getWriter() != null) {
                result.getWriter().flush();
                result.getWriter().close();
            }
        } catch (TransformerException | IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("XML file updated successfully.");
    }

    public static Element getClassElement(String className) {
        Element rootElement = Parser.parseXML(System.getenv("ConfigFile"));
        NodeList classNL = rootElement.getElementsByTagName("class");

        for (int i = 0; i < classNL.getLength(); i++) {
            String classAttrValue = classNL.item(i)
                    .getAttributes()
                    .getNamedItem("class_name")
                    .getNodeValue();
            if (classAttrValue.equals(className)) {
                return (Element) classNL.item(i);
            }
        }
        return null;
    }

    public static String getClassField(Element classElement, String fieldName) {
        NodeList fieldsNL = classElement.getElementsByTagName("field");

        for (int i = 0; i < fieldsNL.getLength(); i++) {
            String fieldAttributeValue = fieldsNL.item(i)
                    .getAttributes()
                    .getNamedItem("field_name")
                    .getNodeValue();
            if (fieldAttributeValue.equals(fieldName)) {
                return fieldsNL.item(i).getTextContent();
            }
        }
        return null;
    }

    public static <T> void assignIfNotNull(Element classElement, String fieldName, Function<String, T> parser, Consumer<T> setter) {
        String value = getClassField(classElement, fieldName);
        if (value != null) {
            setter.accept(parser.apply(value));
        }
    }
}
