/*
 * #%L
 * JBossOSGi SPI
 * %%
 * Copyright (C) 2010 - 2012 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package org.jboss.test.osgi.metadata;

import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.metadata.OSGiMetaDataBuilder;
import org.jboss.osgi.metadata.PackageAttribute;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test the {@link OSGiMetaDataBuilder}.
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Jun-2013
 */
public class OSGiMetaDataBuilderTestCase {

    @Test
    public void testSplitLines() throws Exception {

        ClassLoader classLoader = getClass().getClassLoader();
        InputStream input = classLoader.getResourceAsStream("jbosgi-xservice-01.properties");
        Properties props = new Properties();
        props.load(input);

        OSGiMetaData metadata = OSGiMetaDataBuilder.load(props);
        List<PackageAttribute> exportPackages = metadata.getExportPackages();
        Assert.assertEquals(15, exportPackages.size());
    }

    @Test
    public void testDuplicateExportPackages() throws Exception {

        OSGiMetaDataBuilder builder = OSGiMetaDataBuilder.createBuilder("duplicate.exports");
        builder.addExportPackages("org.acme.foo;version=2.0", "org.acme.foo;version=1.7", "org.acme.foo;version=2.0", "org.acme.bar", "org.acme.bar");
        builder.addImportPackages("org.acme.baz;version=2.0", "org.acme.baz;version=1.7");

        OSGiMetaData metadata = builder.getOSGiMetaData();
        List<PackageAttribute> exportPackages = metadata.getExportPackages();
        Assert.assertEquals(3, exportPackages.size());
        Assert.assertEquals("org.acme.foo", exportPackages.get(0).getAttribute());
        Assert.assertEquals("2.0", exportPackages.get(0).getAttributeValue("version", String.class));
        Assert.assertEquals("org.acme.foo", exportPackages.get(1).getAttribute());
        Assert.assertEquals("1.7", exportPackages.get(1).getAttributeValue("version", String.class));
        Assert.assertEquals("org.acme.bar", exportPackages.get(2).getAttribute());

        // A package can only be imported once
        Assert.assertEquals(1, metadata.getImportPackages().size());
    }
}
