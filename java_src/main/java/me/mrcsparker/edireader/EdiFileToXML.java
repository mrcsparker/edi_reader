package me.mrcsparker.edireader;


import org.apache.commons.io.output.ByteArrayOutputStream;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class EdiFileToXML {
    public String run(String input) throws EdiException {

        InputSource inputSource = null;
        try {
            inputSource = new InputSource(new FileReader(input));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream generatedOutput = new ByteArrayOutputStream();

        try {
            System.setProperty("javax.xml.parsers.SAXParserFactory", "com.berryworks.edireader.EDIParserFactory");
            SAXParserFactory sFactory = SAXParserFactory.newInstance();
            SAXParser sParser = sFactory.newSAXParser();
            XMLReader ediReader = sParser.getXMLReader();

            // Establish the SAXSource
            SAXSource source = new SAXSource(ediReader, inputSource);

            // Establish an XSL Transformer to generate the XML output.
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            // The StreamResult to capture the generated XML output.
            StreamResult result = new StreamResult(generatedOutput);

            // Call the XSL Transformer with no stylesheet to generate
            // XML output from the parsed input.
            transformer.transform(source, result);

            return generatedOutput.toString(StandardCharsets.UTF_8);

        } catch (SAXException e) {
            System.out.println("\nUnable to create EDIReader: " + e);
            throw new EdiException(e.toString());
        } catch (ParserConfigurationException e) {
            System.out.println("\nUnable to create EDIReader: " + e);
            throw new EdiException(e.toString());
        } catch (TransformerConfigurationException e) {
            System.out.println("\nUnable to create Transformer: " + e);
            throw new EdiException(e.toString());
        } catch (TransformerException e) {
            System.out.println("\nFailure to transform: " + e);
            throw new EdiException(e.toString());
        }
    }
}
