package com.paulmandal.atak.forwarder.comm.protobuf.fallback;

import android.util.Xml;

import androidx.annotation.Nullable;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.google.protobuf.InvalidProtocolBufferException;
import com.paulmandal.atak.forwarder.protobufs.LegacyCotEventProtobuf;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;

import atakmap.commoncommo.protobuf.v1.Contact;
import atakmap.commoncommo.protobuf.v1.Detail;
import atakmap.commoncommo.protobuf.v1.Group;
import atakmap.commoncommo.protobuf.v1.Precisionlocation;
import atakmap.commoncommo.protobuf.v1.Status;
import atakmap.commoncommo.protobuf.v1.Takv;
import atakmap.commoncommo.protobuf.v1.Track;


@Deprecated
public class FallbackCotEventProtobufConverter {
    private static final String KEY_DETAIL = "detail";
    private static final String KEY_CONTACT = "contact";
    private static final String KEY_GROUP = "__group";
    private static final String KEY_PRECISIONLOCATION = "precisionlocation";
    private static final String KEY_STATUS = "status";
    private static final String KEY_TAKV = "takv";
    private static final String KEY_TRACK = "track";

    private static final String KEY_CALLSIGN = "callsign";
    private static final String KEY_ENDPOINT = "endpoint";

    private static final String KEY_NAME = "name";
    private static final String KEY_ROLE = "role";

    private static final String KEY_GEOPOINTSRC = "geopointsrc";
    private static final String KEY_ALTSRC = "altsrc";

    private static final String KEY_BATTERY = "battery";
    private static final String KEY_READINESS = "readiness";

    private static final String KEY_DEVICE = "device";
    private static final String KEY_OS = "os";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_VERSION = "version";

    private static final String KEY_COURSE = "course";
    private static final String KEY_SPEED = "speed";

    @Deprecated
    public byte[] toByteArray(CotEvent cotEvent) {
        LegacyCotEventProtobuf.LegacyCotEvent cotEventProtos = cotEventProtosFromCotEvent(cotEvent);
        return cotEventProtos.toByteArray();
    }

    @Nullable
    @Deprecated
    public CotEvent toCotEvent(byte[] cotProtobuf) {
        CotEvent cotEvent = null;
        try {
            LegacyCotEventProtobuf.LegacyCotEvent protoCotEvent = LegacyCotEventProtobuf.LegacyCotEvent.parseFrom(cotProtobuf);
            cotEvent = new CotEvent();
            cotEvent.setAccess(nullForEmptyString(protoCotEvent.getAccess()));
            cotEvent.setHow(nullForEmptyString(protoCotEvent.getHow()));
            cotEvent.setOpex(nullForEmptyString(protoCotEvent.getOpex()));
            cotEvent.setQos(nullForEmptyString(protoCotEvent.getQos()));
            cotEvent.setTime(new CoordinatedTime(protoCotEvent.getSendTime()));
            cotEvent.setStart(new CoordinatedTime(protoCotEvent.getStartTime()));
            cotEvent.setStale(new CoordinatedTime(protoCotEvent.getStaleTime()));
            cotEvent.setType(protoCotEvent.getType());
            cotEvent.setUID(protoCotEvent.getUid());
            cotEvent.setPoint(new CotPoint(protoCotEvent.getLat(), protoCotEvent.getLon(), protoCotEvent.getHae(), protoCotEvent.getCe(), protoCotEvent.getLe()));
            cotEvent.setDetail(cotDetailFromProtoDetail(protoCotEvent.getDetail()));
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return cotEvent;
    }

    private CotDetail cotDetailFromProtoDetail(Detail.LegacyDetail detail) {
        CotDetail cotDetail = new CotDetail();
        Takv.LegacyTakv takv = detail.getTakv();
        if (takv != null
                && (!isNullOrEmpty(takv.getDevice()) || !isNullOrEmpty(takv.getOs()) || !isNullOrEmpty(takv.getPlatform()) || !isNullOrEmpty(takv.getVersion()))) {
            CotDetail takvDetail = new CotDetail(KEY_TAKV);

            if (!isNullOrEmpty(takv.getOs())) {
                takvDetail.setAttribute(KEY_OS, takv.getOs());
            }
            if (!isNullOrEmpty(takv.getVersion())) {
                takvDetail.setAttribute(KEY_VERSION, takv.getVersion());
            }
            if (!isNullOrEmpty(takv.getDevice())) {
                takvDetail.setAttribute(KEY_DEVICE, takv.getDevice());
            }
            if (!isNullOrEmpty(takv.getPlatform())) {
                takvDetail.setAttribute(KEY_PLATFORM, takv.getPlatform());
            }
            cotDetail.addChild(takvDetail);
        }

        Contact.LegacyContact contact = detail.getContact();
        if (contact != null
                && (!isNullOrEmpty(contact.getCallsign()) || !isNullOrEmpty(contact.getEndpoint()))) {
            CotDetail contactDetail = new CotDetail(KEY_CONTACT);
            if (!isNullOrEmpty(contact.getCallsign())) {
                contactDetail.setAttribute(KEY_CALLSIGN, contact.getCallsign());
            }
            if (!isNullOrEmpty(contact.getEndpoint())) {
                contactDetail.setAttribute(KEY_ENDPOINT, contact.getEndpoint());
            }
            cotDetail.addChild(contactDetail);
        }

        String xmlDetailStr = detail.getXmlDetail();
        if (xmlDetailStr != null) {
            xmlDetailStr = "<detail>" + xmlDetailStr + "</detail>";
            xmlDetailStr = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + xmlDetailStr;

            XmlPullParser parser = Xml.newPullParser();
            try {
                parser.setInput(new StringReader(xmlDetailStr));

                parser.next(); // Skip the <detail> tag
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() == XmlPullParser.START_TAG) {
                        cotDetail.addChild(cotDetailFromXml(parser));
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                e.printStackTrace();
            }
        }

        Precisionlocation.LegacyPrecisionLocation precisionLocation = detail.getPrecisionLocation();
        if (precisionLocation != null
                && (!isNullOrEmpty(precisionLocation.getAltsrc()) || !isNullOrEmpty(precisionLocation.getGeopointsrc()))) {
            CotDetail precisionLocationDetail = new CotDetail(KEY_PRECISIONLOCATION);

            if (!isNullOrEmpty(precisionLocation.getAltsrc())) {
                precisionLocationDetail.setAttribute(KEY_ALTSRC, precisionLocation.getAltsrc());
            }
            if (!isNullOrEmpty(precisionLocation.getGeopointsrc())) {
                precisionLocationDetail.setAttribute(KEY_GEOPOINTSRC, precisionLocation.getGeopointsrc());
            }

            cotDetail.addChild(precisionLocationDetail);
        }

        Group.LegacyGroup group = detail.getGroup();
        if (group != null
                && (!isNullOrEmpty(group.getName()) || !isNullOrEmpty(group.getRole()))) {
            CotDetail groupDetail = new CotDetail(KEY_GROUP);
            
            if (!isNullOrEmpty(group.getName())) {
                groupDetail.setAttribute(KEY_NAME, group.getName());
            }
            if (!isNullOrEmpty(group.getRole())) {
                groupDetail.setAttribute(KEY_ROLE, group.getRole());
            }
            
            cotDetail.addChild(groupDetail);
        }

        Status.LegacyStatus status = detail.getStatus();
        if (status != null
                && (status.getBattery() > 0 || !isNullOrEmpty(status.getReadiness()))) {
            CotDetail statusDetail = new CotDetail(KEY_STATUS);

            if (status.getBattery() > 0) {
                statusDetail.setAttribute(KEY_BATTERY, Integer.toString(status.getBattery()));
            }

            if(!isNullOrEmpty(status.getReadiness())) {
                statusDetail.setAttribute(KEY_READINESS, status.getReadiness());
            }
            cotDetail.addChild(statusDetail);
        }

        Track.LegacyTrack track = detail.getTrack();
        if (track != null
                && (track.getCourse() != 0.0 || track.getSpeed() != 0.0)) {
            CotDetail trackDetail = new CotDetail(KEY_TRACK);

            if (track.getCourse() != 0.0) {
                trackDetail.setAttribute(KEY_COURSE, Double.toString(track.getCourse()));
            }
            if (track.getSpeed() != 0.0) {
                trackDetail.setAttribute(KEY_SPEED, Double.toString(track.getSpeed()));
            }
            cotDetail.addChild(trackDetail);
        }

        return cotDetail;
    }

    private CotDetail cotDetailFromXml(XmlPullParser parser) throws IOException, XmlPullParserException {
        CotDetail xmlDetail = new CotDetail(parser.getName());
        for (int i = 0; i < parser.getAttributeCount(); i++) {
            xmlDetail.setAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
        }

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                xmlDetail.addChild(cotDetailFromXml(parser));
            } else if (parser.getEventType() == XmlPullParser.TEXT) {
                xmlDetail.setInnerText(parser.getText());
            }
        }
        return xmlDetail;
    }

    private LegacyCotEventProtobuf.LegacyCotEvent cotEventProtosFromCotEvent(CotEvent cotEvent) {
        LegacyCotEventProtobuf.LegacyCotEvent.Builder builder = LegacyCotEventProtobuf.LegacyCotEvent.newBuilder();

        if (cotEvent.getAccess() != null) {
            builder.setAccess(cotEvent.getAccess());
        }

        if (cotEvent.getQos() != null) {
            builder.setQos(cotEvent.getQos());
        }

        if (cotEvent.getUID() != null) {
            builder.setUid(cotEvent.getUID());
        }

        if (cotEvent.getType() != null) {
            builder.setType(cotEvent.getType());
        }

        if (cotEvent.getOpex() != null) {
            builder.setOpex(cotEvent.getOpex());
        }

        if (cotEvent.getHow() != null) {
            builder.setHow(cotEvent.getHow());
        }

        return builder.setSendTime(cotEvent.getTime().getMilliseconds())
                .setStartTime(cotEvent.getStart().getMilliseconds())
                .setStaleTime(cotEvent.getStale().getMilliseconds())
                .setLat(cotEvent.getCotPoint().getLat())
                .setLon(cotEvent.getCotPoint().getLon())
                .setHae(cotEvent.getCotPoint().getHae())
                .setCe(cotEvent.getCotPoint().getCe())
                .setLe(cotEvent.getCotPoint().getLe())
                .setDetail(detailFromCotDetail(cotEvent.getDetail()))
                .build();
    }

    private Detail.LegacyDetail detailFromCotDetail(CotDetail cotDetail) {
        Detail.LegacyDetail.Builder builder = Detail.LegacyDetail.newBuilder();
        StringBuilder xmlDetail = new StringBuilder();
        if (cotDetail.getElementName().equals(KEY_DETAIL)) { // TODO: do we need this check?
            for (CotDetail innerDetail : cotDetail.getChildren()) {
                switch (innerDetail.getElementName()) {
                    case KEY_CONTACT:
                        builder.setContact(contactFromCotDetail(innerDetail));
                        break;
                    case KEY_GROUP:
                        builder.setGroup(groupFromCotDetail(innerDetail));
                        break;
                    case KEY_PRECISIONLOCATION:
                        builder.setPrecisionLocation(precisionLocationFromCotDetail(innerDetail));
                        break;
                    case KEY_STATUS:
                        builder.setStatus(statusFromCotDetail(innerDetail));
                        break;
                    case KEY_TAKV:
                        builder.setTakv(takvFromCotDetail(innerDetail));
                        break;
                    case KEY_TRACK:
                        builder.setTrack(trackFromCotDetail(innerDetail));
                        break;
                    default:
                        innerDetail.buildXml(xmlDetail);
                        break;
                }
            }
            if (xmlDetail.length() > 0) {
                builder.setXmlDetail(xmlDetail.toString());
            }
        }
        return builder.build();
    }

    private Contact.LegacyContact contactFromCotDetail(CotDetail cotDetail) {
        Contact.LegacyContact.Builder builder = Contact.LegacyContact.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_CALLSIGN:
                    builder.setCallsign(attribute.getValue());
                    break;
                case KEY_ENDPOINT:
                    builder.setEndpoint(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Group.LegacyGroup groupFromCotDetail(CotDetail cotDetail) {
        Group.LegacyGroup.Builder builder = Group.LegacyGroup.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_NAME:
                    builder.setName(attribute.getValue());
                    break;
                case KEY_ROLE:
                    builder.setRole(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Precisionlocation.LegacyPrecisionLocation precisionLocationFromCotDetail(CotDetail cotDetail) {
        Precisionlocation.LegacyPrecisionLocation.Builder builder = Precisionlocation.LegacyPrecisionLocation.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_GEOPOINTSRC:
                    builder.setGeopointsrc(attribute.getValue());
                    break;
                case KEY_ALTSRC:
                    builder.setAltsrc(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Status.LegacyStatus statusFromCotDetail(CotDetail cotDetail) {
        Status.LegacyStatus.Builder builder = Status.LegacyStatus.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_BATTERY:
                    builder.setBattery(Integer.parseInt(attribute.getValue()));
                    break;
                case KEY_READINESS:
                    builder.setReadiness(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Takv.LegacyTakv takvFromCotDetail(CotDetail cotDetail) {
        Takv.LegacyTakv.Builder builder = Takv.LegacyTakv.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_DEVICE:
                    builder.setDevice(attribute.getValue());
                    break;
                case KEY_OS:
                    builder.setOs(attribute.getValue());
                    break;
                case KEY_PLATFORM:
                    builder.setPlatform(attribute.getValue());
                    break;
                case KEY_VERSION:
                    builder.setVersion(attribute.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private Track.LegacyTrack trackFromCotDetail(CotDetail cotDetail) {
        Track.LegacyTrack.Builder builder = Track.LegacyTrack.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_COURSE:
                    builder.setCourse(Double.parseDouble(attribute.getValue()));
                    break;
                case KEY_SPEED:
                    builder.setSpeed(Double.parseDouble(attribute.getValue()));
                    break;
            }
        }
        return builder.build();
    }

    private String nullForEmptyString(String s) {
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
