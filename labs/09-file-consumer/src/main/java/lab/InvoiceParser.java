package lab;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/** Extracts an invoice title with the JDK DOM parser. Used by the preview endpoint. */
@Component
public class InvoiceParser {
    private final Fixture fixture;

    public InvoiceParser(Fixture fixture) {
        this.fixture = fixture;
    }

    DocumentBuilder builder(boolean hardened) {
        try {
            var factory = DocumentBuilderFactory.newDefaultInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute("jdk.xml.maxElementDepth", 32);

            // The guided repair: this invoice format has no DOCTYPE, so the parser may not
            // process one or load anything it names. Permissive mode reproduces the flaw.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", hardened);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", !hardened);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", !hardened);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", !hardened);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, hardened ? "" : "file,http");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            var builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> {
                if (hardened) {
                    fixture.resolutions.incrementAndGet();
                    throw new SAXException("External reference denied");
                }
                fixture.confine(systemId);
                // Returning null does NOT deny resolution: the parser opens the URI itself.
                return null;
            });
            builder.setErrorHandler(new DefaultHandler() {
                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            return builder;
        } catch (ParserConfigurationException | IllegalArgumentException e) {
            // A required setting is unavailable: fail closed instead of retrying without it.
            throw new IllegalStateException("Required XML policy unavailable", e);
        }
    }

    public String title(byte[] bytes, boolean hardened) {
        DocumentBuilder builder = builder(hardened);
        try {
            Element root = builder.parse(new ByteArrayInputStream(bytes)).getDocumentElement();
            if (!plain(root, "invoice")) {
                throw new SAXException("Expected invoice");
            }
            Element title = null;
            for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() == Node.TEXT_NODE && node.getTextContent().isBlank()) {
                    continue;
                }
                if (!(node instanceof Element element) || title != null || !plain(element, "title")) {
                    throw new SAXException("Expected one title");
                }
                title = element;
            }
            if (title == null) {
                throw new SAXException("Title missing");
            }
            for (Node node = title.getFirstChild(); node != null; node = node.getNextSibling()) {
                short type = node.getNodeType();
                if (type != Node.TEXT_NODE && type != Node.CDATA_SECTION_NODE) {
                    throw new SAXException("Title must be text");
                }
            }
            String text = title.getTextContent();
            if (text.isBlank() || text.length() > 120) {
                throw new SAXException("Title length");
            }
            return text;
        } catch (SAXException | IOException e) {
            throw new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid invoice XML");
        }
    }

    private static boolean plain(Element element, String name) {
        return name.equals(element.getTagName())
                && element.getNamespaceURI() == null
                && !element.hasAttributes();
    }
}
