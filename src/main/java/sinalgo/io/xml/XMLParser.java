/*
BSD 3-Clause License

Copyright (c) 2007-2013, Distributed Computing Group (DCG)
                         ETH Zurich
                         Switzerland
                         dcg.ethz.ch
              2017-2018, André Brait

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package sinalgo.io.xml;

import lombok.Getter;
import lombok.Setter;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import sinalgo.configuration.Configuration;
import sinalgo.exception.SinalgoFatalException;
import sinalgo.runtime.Global;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

/**
 * This is the class responsible for the parsing and storing of the xml-file. It
 * parses a xml-file with a given filename.
 * <p>
 * Attention: If you wonder, why I don't use the logging mechanism in this file,
 * this has a reason. You are not allowed to use the logging mechanism before
 * the -overwrite parameters are processed. I.e. it is possible (or even highly
 * probable) that a user changes the outputfile.
 */
public class XMLParser {

    /**
     * This boolean indicates whether the parse() call has to be done or not. When
     * the blockParse flag has been set, the parsing is not done and the parse
     * method returns without doing nothing.
     * <p>
     * This means: When the project Selector has already provided all settings, the
     * config file won't be parsed again in the main method.
     */
    @Getter
    @Setter
    private static boolean blockParse;

    /**
     * This method parses the framework node of the xml configuration file. All
     * entries are supposed to be immediate children of the <framework> tag.
     *
     * @param framework The element from the xml structure to parse.
     */
    private static void parseFrameworkConfig(Element framework) {
        List<?> children = framework.getChildren();
        for (Object aChildren : children) {
            Element child = (Element) aChildren;
            String fieldName = child.getName();

            // test whether this child has attributes
            if (child.getAttributes().size() <= 0) { // this tag has no attributes
                continue;
            }
            String value = child.getAttributeValue("value");
            if (value == null) { // did not find
                throw new SinalgoFatalException(
                        "Error while parsing the configuration file: The entry " +
                                fieldName + "' contains attributes, but none is called 'value'.");
            }
            Configuration.setFrameworkConfigurationEntry(fieldName, value);
        }
    }

    /**
     * This method parses a custom node of the xml structure.
     * <p>
     * This method is also called by the project selector to read the custom
     * settings
     *
     * @param current The element from the xml file to parse, initially the <Custom>
     *                element
     * @param path    The path to the custom element in the xml-file to identify it
     *                later.
     */
    public static void parseCustom(Element current, String path) {
        List<?> children = current.getChildren();
        for (Object aChildren : children) {
            Element child = (Element) aChildren;

            for (Object attr : child.getAttributes()) {
                Attribute a = (Attribute) attr;
                if ("value".equals(a.getName().toLowerCase())) {
                    Configuration.putPropertyEntry(path + child.getName().toLowerCase(), a.getValue());
                } else {
                    Configuration.putPropertyEntry(
                            path + child.getName().toLowerCase() + "/" + a.getName().toLowerCase(), a.getValue());
                }
            }
            if (child.getChildren().size() > 0) { // recursive call on all children
                parseCustom(child, path + child.getName().toLowerCase() + "/");
            }
        }
    }

    /**
     * This method parses a xml-file and stores the data in the configuration.
     *
     * @param reader The input reader of the xml-file.
     */
    public static void parse(Reader reader) {
        if (!isBlockParse()) {
            try {
                Document doc = new SAXBuilder().build(reader);
                Element root = doc.getRootElement();
                Element framework = root.getChild("Framework");
                Element custom = root.getChild("Custom");
                if (framework == null) {
                    throw new SinalgoFatalException("Corrupt XML configuration file: The element '<Framework>' is missing.");
                }
                if (custom == null) {
                    throw new SinalgoFatalException("Corrupt XML configuration file: The element '<Custom>' is missing.");
                }
                parseFrameworkConfig(framework);
                parseCustom(custom, "");
            } catch (JDOMException e) {
                throw new SinalgoFatalException("Currupt XML configuration file (" + reader + ").", e);
            } catch (IOException e) {
                if (Global.isUseProject()) {
                    throw new SinalgoFatalException("Cannot find the XML-configuration file (" + reader + ").", e);
                }
            }
        }
    }

}
