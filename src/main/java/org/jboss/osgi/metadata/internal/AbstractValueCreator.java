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

/**
 * Abstract value creator. Extend this one for safe string usage.
 * 
 * @author <a href="mailto:ales.justin@jboss.com">Ales Justin</a>
 * @author Thomas.Diesler@jboss.com
 */
abstract class AbstractValueCreator<T> implements ValueCreator<T> {

    private boolean trim;

    protected AbstractValueCreator() {
        this(false);
    }

    protected AbstractValueCreator(boolean trim) {
        this.trim = trim;
    }

    public T createValue(String attribute) {
        if (attribute == null)
            return null;
        if (trim)
            attribute = attribute.trim();
        return useString(attribute);
    }

    /**
     * Use this method on non-null trimmed string.
     * 
     * @param attibute non-null trimmed string
     * @return expected value
     */
    protected abstract T useString(String attibute);
}
