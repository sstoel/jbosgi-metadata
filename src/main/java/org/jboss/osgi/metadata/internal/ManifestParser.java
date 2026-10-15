/*
 * #%L
 * JBossOSGi Resolver Metadata
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.jboss.osgi.metadata.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jboss.osgi.metadata.PackageAttribute;
import org.jboss.osgi.metadata.Parameter;
import org.jboss.osgi.metadata.ParameterizedAttribute;
import org.jboss.osgi.metadata.spi.ElementParser;
import org.jboss.osgi.metadata.spi.NotNullException;

/**
 * ManifestParser.
 *
 * @author <a href="adrian@jboss.com">Adrian Brock</a>
 * @author Thomas.Diesler@jboss.com
 */
public class ManifestParser {
    /**
     * Parse packages
     *
     * @param header the header
     * @param list the list of packages to create
     */
    public static void parsePackages(String header, List<PackageAttribute> list) {
        parse(header, list, true, false);
    }

    /**
     * Parse parameters
     *
     * @param header the header
     * @param list the list of parameters to create
     */
    public static void parseParameters(String header, List<ParameterizedAttribute> list) {
        parse(header, list, false, false);
    }

    /**
     * Parse paths
     *
     * @param header the header
     * @param list the list of paths to create
     */
    public static void parsePaths(String header, List<ParameterizedAttribute> list) {
        parse(header, list, false, false);
    }

    /**
     * Parse a header
     *
     * @param header the header
     * @param list the list to create
     * @param packages whether to create packages
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void parse(String header, List list, boolean packages, boolean allowDuplicateAttributes) {
        NotNullException.assertValue(header, "header");
        if (header.length() == 0)
            throw new IllegalArgumentException("Invalid null header");

        // Split the header into clauses using which are seperated by commas
        // Like this: path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2,
        // path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
        List<String> clauses = ElementParser.parseDelimitedString(header, ',');

        // Now parse each clause
        for (String clause : clauses) {
            // Split the clause into paths, directives and attributes using the semi-colon
            // Like this: path; path; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
            List<String> pieces = ElementParser.parseDelimitedString(clause, ';');

            // Collect the paths they should be first
            List<String> paths = new ArrayList<String>();
            for (String piece : pieces) {
                if (indexOfUnquoted(piece, '=') >= 0)
                    break;
                paths.add(unquote(piece));
            }
            if (paths.isEmpty())
                throw new IllegalArgumentException("No paths for clause: " + clause);

            Map<String, Parameter> directives = null;
            Map<String, Parameter> attributes = null;

            for (int i = paths.size(); i < pieces.size(); ++i) {
                String piece = pieces.get(i);
                // The name ends at the first '=' outside a quoted string, a directive when it is preceded by ':'
                int seperator = indexOfUnquoted(piece, '=');
                if (seperator > 0 && piece.charAt(seperator - 1) == ':') {
                    String name = piece.substring(0, seperator - 1);
                    String value = piece.substring(seperator + 1);
                    if (directives == null)
                        directives = new LinkedHashMap<String, Parameter>();
                    name = unquote(name.trim());
                    if (directives.containsKey(name))
                        throw new IllegalArgumentException("Duplicate directive: " + name);
                    value = unquote(value.trim());
                    directives.put(name, new AbstractParameter(ValueCreatorUtil.STRING_VC, value));
                } else {
                    if (seperator >= 0) {
                        String name = piece.substring(0, seperator);
                        String value = piece.substring(seperator + 1);
                        ValueCreator<?> vc = ValueCreatorUtil.STRING_VC;
                        if (attributes == null)
                            attributes = new LinkedHashMap<String, Parameter>();
                        name = unquote(name.trim());
                        seperator = name.indexOf(":");
                        if (seperator >= 0) {
                            String type = name.substring(seperator + 1).trim();
                            if (type.startsWith("List")) {
                                int index = type.indexOf('<');
                                int endIndex = type.indexOf('>');
                                if (index > 0 && endIndex < index)
                                    throw new IllegalArgumentException("Invalid attribute type: " + type);
                                String subtype = index > 0 ? type.substring(index + 1, endIndex) : "String";
                                type = "List<" + subtype.trim() + ">";
                            }
                            vc = ValueCreatorUtil.forType(type);
                            name = name.substring(0, seperator).trim();
                        }
                        Parameter attribute = attributes.get(name);
                        if (attribute != null) {
                            if (!allowDuplicateAttributes) {
                                throw new IllegalArgumentException("Duplicate attribute: " + name);
                            }
                        } else {
                            attribute = new AbstractParameter(vc);
                            attributes.put(name, attribute);
                        }
                        value = unquote(value.trim());
                        attribute.addValue(value);
                    } else {
                        throw new IllegalArgumentException("Path [" + piece + "] should appear before attributes and directives in clause: " + clause);
                    }
                }
            }

            for (String path : paths) {
                ParameterizedAttribute metadata = null;
                if (packages)
                    metadata = new AbstractPackageAttribute(path, attributes, directives);
                else
                    metadata = new AbstractParameterizedAttribute(path, attributes, directives);

                list.add(metadata);
            }
        }
    }

    /**
     * Remove around quotes around a string
     *
     * @param string the string
     * @return the unquoted string
     */
    private static String unquote(String string) {
        if (string.length() < 2)
            return string;
        char quote = string.charAt(0);
        if ((quote == '\"' || quote == '\'') && string.charAt(string.length() - 1) == quote)
            return unescape(string.substring(1, string.length() - 1));
        return string;
    }

    /**
     * Resolve the escapes of a quoted string: a backslash followed by a quote or a backslash (1.3.2)
     */
    private static String unescape(String string) {
        if (string.indexOf('\\') < 0)
            return string;
        StringBuilder result = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\\' && i + 1 < string.length()) {
                char next = string.charAt(i + 1);
                if (next == '\\' || next == '"' || next == '\'') {
                    result.append(next);
                    i++;
                    continue;
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    private static int indexOfUnquoted(String string, char search) {
        char quote = 0;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c == '\\' && quote != 0)
                i++;
            else if (quote != 0)
                quote = c == quote ? 0 : quote;
            else if (c == '"' || c == '\'')
                quote = c;
            else if (c == search)
                return i;
        }
        return -1;
    }

}
