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
package org.jboss.osgi.metadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes.Name;

import org.jboss.osgi.metadata.spi.AbstractOSGiMetaData;
import org.jboss.osgi.metadata.spi.NotNullException;

/**
 * OSGi meta data of a map of headers, like the headers of an OSGi Connect content.
 *
 * Unlike manifest attributes the header names are not restricted to the names of JAR manifest attributes.
 *
 * @since 16-Oct-2026
 */
class HeadersMetaDataInternal extends AbstractOSGiMetaData {
    private final Map<String, String> headers;

    HeadersMetaDataInternal(Map<String, String> headers) {
        NotNullException.assertValue(headers, "headers");
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, String>(headers));
    }

    @Override
    protected Map<String, String> getHeaderMap() {
        return headers;
    }

    @Override
    public Map<Name, String> getMainAttributes() {
        // The headers whose names are also manifest attribute names
        Map<Name, String> result = new LinkedHashMap<Name, String>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            try {
                result.put(new Name(entry.getKey()), entry.getValue());
            } catch (IllegalArgumentException ex) {
                // not a manifest attribute name
            }
        }
        return result;
    }

    @Override
    public String getMainAttribute(String key) {
        String value = headers.get(key);
        if (value == null) {
            // Header names are case insensitive
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(key)) {
                    value = entry.getValue();
                    break;
                }
            }
        }
        return value != null ? value.trim() : null;
    }
}
