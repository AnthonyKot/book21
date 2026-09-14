package lab;
import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT;
@Component
public class InvoiceParser {
    private final Fixture fixture;
    public InvoiceParser(Fixture fixture){this.fixture=fixture;}
    DocumentBuilder builder(boolean hardened) {
        try {
            var f=DocumentBuilderFactory.newDefaultInstance();
            f.setNamespaceAware(true);f.setXIncludeAware(false);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            f.setAttribute("jdk.xml.maxElementDepth",32);
            // Both modes have a depth limit. Vulnerable mode deliberately permits fixture entities.
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",hardened);
            f.setFeature("http://xml.org/sax/features/external-general-entities",!hardened);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities",!hardened);
            f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",!hardened);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,hardened ? "" : "file,http");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            var b=f.newDocumentBuilder();
            b.setEntityResolver((publicId,systemId)->{
                fixture.resolutions.incrementAndGet();
                if(hardened || !fixture.permits(systemId))throw new SAXException("External reference denied");
                // null delegates actual access to the parser; this does NOT deny resolution.
                return null;
            });
            b.setErrorHandler(new DefaultHandler(){
                @Override public void error(SAXParseException e)throws SAXException{throw e;}
                @Override public void fatalError(SAXParseException e)throws SAXException{throw e;}
            });
            return b;
        }catch(ParserConfigurationException | IllegalArgumentException e){
            throw new IllegalStateException("Required XML policy unavailable",e);
        }
    }
    public String title(byte[] bytes,boolean hardened) {
        // Configuration failures stay server failures; never retry with an unconfigured parser.
        var b=builder(hardened);
        try {
            var doc=b.parse(new ByteArrayInputStream(bytes));
            var root=doc.getDocumentElement();
            if(!plain(root,"invoice"))throw new SAXException("Expected invoice");
            Element title=null;
            for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
                if(n.getNodeType()==Node.TEXT_NODE && n.getTextContent().isBlank())continue;
                if(!(n instanceof Element e) || title!=null || !plain(e,"title"))throw new SAXException("Expected one title");
                title=e;
            }
            if(title==null)throw new SAXException("Title missing");
            for(Node n=title.getFirstChild();n!=null;n=n.getNextSibling())
                if(n.getNodeType()!=Node.TEXT_NODE && n.getNodeType()!=Node.CDATA_SECTION_NODE)throw new SAXException("Text only");
            String text=title.getTextContent();
            if(text.isBlank()||text.length()>120)throw new SAXException("Title length");
            return text;
        }catch(SAXException | IOException e){throw new ResponseStatusException(UNPROCESSABLE_CONTENT,"Invalid invoice XML");}
    }
    private static boolean plain(Element e,String name){return name.equals(e.getTagName()) && e.getNamespaceURI()==null && !e.hasAttributes();}
}
