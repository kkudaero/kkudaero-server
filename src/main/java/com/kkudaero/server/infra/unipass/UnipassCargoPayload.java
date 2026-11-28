package com.kkudaero.server.infra.unipass;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public record UnipassCargoPayload(int totalCount, String notice, List<Summary> summaries, List<Detail> details) {

    private static final String MULTIPLE_RESULT_NOTICE_PREFIX = "[N00]";
    private static final String TOTAL_COUNT_TAG = "tCnt";
    private static final String NOTICE_TAG = "ntceInfo";
    private static final String SUMMARY_TAG = "cargCsclPrgsInfoQryVo";
    private static final String DETAIL_TAG = "cargCsclPrgsInfoDtlQryVo";
    private static final String CARGO_MANAGEMENT_NUMBER_TAG = "cargMtNo";
    private static final String CLEARANCE_STATUS_TAG = "csclPrgsStts";
    private static final String PROGRESS_STATUS_TAG = "prgsStts";
    private static final String PROCESSED_AT_TAG = "prcsDttm";
    private static final String ARRIVAL_DATE_TAG = "etprDt";
    private static final String EVENT_TYPE_TAG = "cargTrcnRelaBsopTpcd";
    private static final String SHED_NAME_TAG = "shedNm";
    private static final String RELEASE_CONTENT_TAG = "rlbrCn";
    private static final String DISALLOW_DOCTYPE_FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";

    public static UnipassCargoPayload parse(byte[] xml) {
        Element root = parseDocument(xml).getDocumentElement();
        List<Summary> summaries = new ArrayList<>();
        for (Element summary : children(root, SUMMARY_TAG)) {
            summaries.add(new Summary(
                    firstText(summary, CARGO_MANAGEMENT_NUMBER_TAG),
                    firstText(summary, CLEARANCE_STATUS_TAG),
                    firstText(summary, PROGRESS_STATUS_TAG),
                    firstText(summary, PROCESSED_AT_TAG),
                    firstText(summary, ARRIVAL_DATE_TAG)
            ));
        }
        List<Detail> details = new ArrayList<>();
        for (Element detail : children(root, DETAIL_TAG)) {
            details.add(new Detail(
                    firstText(detail, EVENT_TYPE_TAG),
                    firstText(detail, PROCESSED_AT_TAG),
                    firstText(detail, SHED_NAME_TAG),
                    firstText(detail, RELEASE_CONTENT_TAG)
            ));
        }
        return new UnipassCargoPayload(
                parseTotalCount(firstText(root, TOTAL_COUNT_TAG)),
                firstText(root, NOTICE_TAG),
                summaries,
                details
        );
    }

    public boolean isError() {
        return totalCount < 0;
    }

    public boolean hasMultipleCargos() {
        return notice.startsWith(MULTIPLE_RESULT_NOTICE_PREFIX);
    }

    private static Document parseDocument(byte[] xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_FEATURE, true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new UnipassUnavailableException("통관 진행 응답을 읽을 수 없습니다.");
        }
    }

    private static int parseTotalCount(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new UnipassUnavailableException("통관 진행 응답에 결과 건수가 없습니다.");
        }
    }

    private static List<Element> children(Element root, String tag) {
        NodeList nodes = root.getChildNodes();
        List<Element> elements = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            if (nodes.item(index) instanceof Element element && tag.equals(element.getTagName())) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static String firstText(Element element, String tag) {
        Node node = element.getElementsByTagName(tag).item(0);
        return node == null ? "" : node.getTextContent().strip();
    }

    public record Summary(String cargMtNo, String csclPrgsStts, String prgsStts, String prcsDttm, String etprDt) {
    }

    public record Detail(String cargTrcnRelaBsopTpcd, String prcsDttm, String shedNm, String rlbrCn) {
    }
}
