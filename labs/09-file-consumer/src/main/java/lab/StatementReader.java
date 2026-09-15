package lab;

import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Totals a partner statement with the JDK streaming (StAX) parser.
 *
 * <pre>{@code
 * <statement>
 *   <line cents="1200">Toner &amp; paper</line>
 *   <line cents="300">Delivery</line>
 * </statement>
 * }</pre>
 */
@Component
public class StatementReader {
    private final Fixture fixture;

    public StatementReader(Fixture fixture) {
        this.fixture = fixture;
    }

    /** Returns, for example, {@code 2 lines, 1500 cents: Toner & paper; Delivery}. */
    public String totals(byte[] bytes) {
        try {
            XMLStreamReader xml = factory().createXMLStreamReader(new ByteArrayInputStream(bytes));
            try {
                return read(xml);
            } finally {
                xml.close();
            }
        } catch (XMLStreamException e) {
            throw invalid();
        }
    }

    private XMLInputFactory factory() {
        XMLInputFactory factory = XMLInputFactory.newDefaultFactory();
        // Lab safety guard (see Fixture.confine).
        factory.setXMLResolver((publicId, systemId, baseUri, namespace) -> {
            try {
                fixture.confine(systemId);
            } catch (IOException e) {
                throw new XMLStreamException(e);
            }
            return null;
        });
        return factory;
    }

    private String read(XMLStreamReader xml) throws XMLStreamException {
        int event = xml.getEventType();
        while (event != XMLStreamConstants.START_ELEMENT) {
            // Skip the prolog before the root element.
            if (!xml.hasNext()) {
                throw invalid();
            }
            event = xml.next();
        }
        requireElement(xml, "statement", 0);

        List<String> items = new ArrayList<>();
        long totalCents = 0;
        while (xml.nextTag() == XMLStreamConstants.START_ELEMENT) {
            requireElement(xml, "line", 1);
            String cents = xml.getAttributeValue(null, "cents");
            if (cents == null || !cents.matches("[0-9]{1,9}")) {
                throw invalid();
            }
            String description = xml.getElementText();
            if (description.isBlank() || description.length() > 120) {
                throw invalid();
            }
            totalCents += Long.parseLong(cents);
            items.add(description);
        }
        while (xml.hasNext()) {
            xml.next();
        }
        if (items.isEmpty()) {
            throw invalid();
        }
        String lines = items.size() == 1 ? "1 line" : items.size() + " lines";
        return lines + ", " + totalCents + " cents: " + String.join("; ", items);
    }

    private static void requireElement(XMLStreamReader xml, String name, int attributes) {
        String namespace = xml.getNamespaceURI();
        if (!name.equals(xml.getLocalName())
                || namespace != null && !namespace.isEmpty()
                || xml.getAttributeCount() != attributes) {
            throw invalid();
        }
    }

    private static ResponseStatusException invalid() {
        return new ResponseStatusException(UNPROCESSABLE_CONTENT, "Invalid statement XML");
    }
}
