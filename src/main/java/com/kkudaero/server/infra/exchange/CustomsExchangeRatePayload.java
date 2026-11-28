package com.kkudaero.server.infra.exchange;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public record CustomsExchangeRatePayload(String resultCode, List<Item> items) {

    private static final String SUCCESS_CODE = "00";
    private static final String RESULT_CODE_TAG = "resultCode";
    private static final String CURRENCY_TAG = "currSgn";
    private static final String RATE_TAG = "fxrt";
    private static final String APPLIED_FROM_TAG = "aplyBgnDt";
    private static final String DISALLOW_DOCTYPE_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";

    public static CustomsExchangeRatePayload parse(String xml) {
        Element root = parseDocument(xml).getDocumentElement();
        NodeList currencies = root.getElementsByTagName(CURRENCY_TAG);
        List<Item> items = new ArrayList<>();
        for (int index = 0; index < currencies.getLength(); index++) {
            Node parent = currencies.item(index).getParentNode();
            if (parent instanceof Element item) {
                items.add(new Item(
                        firstText(item, CURRENCY_TAG),
                        firstText(item, RATE_TAG),
                        firstText(item, APPLIED_FROM_TAG)
                ));
            }
        }
        return new CustomsExchangeRatePayload(firstText(root, RESULT_CODE_TAG), items);
    }

    public boolean isSuccessful() {
        return SUCCESS_CODE.equals(resultCode);
    }

    private static Document parseDocument(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_FEATURE, true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new ExchangeRateUnavailableException("환율 응답을 읽을 수 없습니다.");
        }
    }

    private static String firstText(Element element, String tag) {
        Node node = element.getElementsByTagName(tag).item(0);
        return node == null ? "" : node.getTextContent().strip();
    }

    public record Item(String currSgn, String fxrt, String aplyBgnDt) {
    }
}
