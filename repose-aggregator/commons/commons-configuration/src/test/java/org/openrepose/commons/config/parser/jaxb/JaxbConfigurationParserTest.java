/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
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
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.commons.config.parser.jaxb;

import org.junit.Test;
import org.openrepose.commons.config.parser.common.ConfigurationParser;
import org.openrepose.commons.config.resource.ConfigurationResource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JaxbConfigurationParserTest {

    @Test
    public void shouldReadConfigurationResource() throws JAXBException, IOException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        ConfigurationParser<Element> parser = new JaxbConfigurationParser<>(Element.class, jaxbContext, null);

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createConfig().getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        Element element = parser.read(cfgResource);

        assertNotNull(element);
    }

    @Test(expected = ClassCastException.class)
    public void testRead() throws JAXBException, IOException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(Element.class);
        ConfigurationParser<String> parser = new JaxbConfigurationParser<>(String.class, jaxbContext, null);

        ConfigurationResource cfgResource = mock(ConfigurationResource.class);
        ByteArrayInputStream cfgStream = new ByteArrayInputStream(createConfig().getBytes());
        when(cfgResource.newInputStream()).thenReturn(cfgStream);

        parser.read(cfgResource);
    }

    private static String createConfig() {
        return createConfig("World");
    }

    private static String createConfig(String helloMsg) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<element>\n" +
            "    <hello>" + helloMsg + "</hello>\n" +
            "    <goodbye>See ya.</goodbye>\n" +
            "</element>\n";
    }
}
