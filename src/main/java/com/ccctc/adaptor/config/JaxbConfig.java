package com.ccctc.adaptor.config;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * Created by james on 4/26/17.
 */
@Configuration
public class JaxbConfig {

    @Bean(name="transcriptJAXBContext")
    public JAXBContext transcriptJAXBContext() throws JAXBException {
        return JAXBContextFactory.createContext("com.ccctc.message.collegetranscript.v1_6.impl", null, null);
    }

    @Bean(name="cccExtensionsContext")
    public JAXBContext cccExtensionsContext() throws JAXBException {
        return JAXBContextFactory.createContext("com.xap.ccctran.impl",null,null);
    }
}