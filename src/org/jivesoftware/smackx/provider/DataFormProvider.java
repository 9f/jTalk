package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.MediaElement;
import org.jivesoftware.smackx.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

/**
 * The DataFormProvider parses DataForm packets.
 * 
 * @author Gaston Dombiak
 */
public class DataFormProvider implements PacketExtensionProvider {

    /**
     * Creates a new DataFormProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public DataFormProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        boolean done = false;
        DataForm dataForm = new DataForm(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("instructions")) { 
                    dataForm.addInstruction(parser.nextText());
                }
                else if (parser.getName().equals("title")) {                    
                    dataForm.setTitle(parser.nextText());
                }
                else if (parser.getName().equals("field")) {                    
                    dataForm.addField(parseField(parser));
                }
                else if (parser.getName().equals("item")) {                    
                    dataForm.addItem(parseItem(parser));
                }
                else if (parser.getName().equals("reported")) {                    
                    dataForm.setReportedData(parseReported(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(dataForm.getElementName())) {
                    done = true;
                }
            }
        }
        return dataForm;
    }

    private FormField parseField(XmlPullParser parser) throws Exception {
        boolean done = false;
        FormField formField = new FormField(parser.getAttributeValue("", "var"));
        formField.setLabel(parser.getAttributeValue("", "label"));
        formField.setType(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("desc")) { 
                    formField.setDescription(parser.nextText());
                }
                else if (parser.getName().equals("value")) {                    
                    formField.addValue(parser.nextText());
                }
                else if (parser.getName().equals("media")) {
                	MediaElement me = parseMedia(parser);
                	if (me != null) formField.addMedia(me);
                }
                else if (parser.getName().equals("required")) {                    
                    formField.setRequired(true);
                }
                else if (parser.getName().equals("option")) {                    
                    formField.addOption(parseOption(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("field")) {
                    done = true;
                }
            }
        }
        return formField;
    }

    private DataForm.Item parseItem(XmlPullParser parser) throws Exception {
        boolean done = false;
        List<FormField> fields = new ArrayList<FormField>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    done = true;
                }
            }
        }
        return new DataForm.Item(fields);
    }

    private DataForm.ReportedData parseReported(XmlPullParser parser) throws Exception {
        boolean done = false;
        List<FormField> fields = new ArrayList<FormField>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("reported")) {
                    done = true;
                }
            }
        }
        return new DataForm.ReportedData(fields);
    }

    private FormField.Option parseOption(XmlPullParser parser) throws Exception {
        boolean done = false;
        FormField.Option option = null;
        String label = parser.getAttributeValue("", "label");
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("value")) {
                    option = new FormField.Option(label, parser.nextText());                     
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("option")) {
                    done = true;
                }
            }
        }
        return option;
    }
    
    private MediaElement parseMedia(XmlPullParser parser) throws Exception {
        boolean done = false;
        MediaElement media = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("uri")) {
                	String type = parser.getAttributeValue("", "type");
                	String cid  = parser.nextText();
                	media = new MediaElement(type, cid);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("media")) {
                    done = true;
                }
            }
        }
        return media;
    }
}
