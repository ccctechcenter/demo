/*
 * Copyright (c) 2017. California Community Colleges Technology Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ccctc.adaptor.util.transcript;

import org.xml.sax.SAXException;

import javax.naming.OperationNotSupportedException;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: sallen
 * Date: 7/5/2014
 * Time: 1:46 AM
 * To change this template use File | Settings | File Templates.
 */
public final class ValidationUtils {

    private static Logger logger = Logger.getLogger(ValidationUtils.class.getName());

    public static Schema getSchema( XmlFileType fileType, XmlSchemaVersion version) throws OperationNotSupportedException, SAXException {

        String xsdResourceName = new StringBuilder("/xsd/pesc/")
                .append(fileType.getFilenamePrefix())
                .append("_")
                .append(version.getVersionText())
                .append(".xsd").toString();

        URL schemaFile = ValidationUtils.class.getResource(xsdResourceName);
        if(schemaFile == null) {
            throw new OperationNotSupportedException("Unknown schema: " + xsdResourceName);
        }

        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        return schemaFactory.newSchema(schemaFile);
    }


    /**
     * Validates a PESC XML transcript against the appropriate schema specified by the fileType and version.
     * @param is InputStream containing the contents of the file to be validated
     * @param fileType whether the PESC Transcript is a High School or College transcript
     * @param version which version of the HS/College schema to validate with
     * @throws OperationNotSupportedException thrown if
     * @throws SAXException
     */
    public static void validateDocument(InputStream is, XmlFileType fileType, XmlSchemaVersion version) throws OperationNotSupportedException, SAXException, IOException {

        Source xmlFile = new StreamSource(is);
        Schema schema = ValidationUtils.getSchema(fileType, version);
        javax.xml.validation.Validator validator = schema.newValidator();
        validator.validate(xmlFile);

    }
}
