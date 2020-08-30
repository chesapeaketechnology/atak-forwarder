package com.paulmandal.atak.forwarder.comm.protobuf;

import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.google.protobuf.ByteString;
import com.paulmandal.atak.forwarder.comm.protobuf.gzip.GzipHelper;
import com.paulmandal.atak.forwarder.protobufs.ProtobufFreehandLink;

import java.io.IOException;

public class FreehandLinkProtobufConverter {
    private static final String KEY_LINK = "link";

    private static final String KEY_LINE = "line";

    public ProtobufFreehandLink.MinimalFreehandLink toFreehandLink(CotDetail cotDetail) throws UnknownDetailFieldException {
        ProtobufFreehandLink.MinimalFreehandLink.Builder builder = ProtobufFreehandLink.MinimalFreehandLink.newBuilder();
        CotAttribute[] attributes = cotDetail.getAttributes();
        for (CotAttribute attribute : attributes) {
            switch (attribute.getName()) {
                case KEY_LINE:
                    try {
                        builder.setLine(ByteString.copyFrom(GzipHelper.compress(attribute.getValue()))); // TODO: evaluate this using EXI instead
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    throw new UnknownDetailFieldException("Don't know how to handle detail field: freehandlink." + attribute.getName());
            }
        }
        return builder.build();
    }

    public void maybeAddFreehandLink(CotDetail cotDetail, ProtobufFreehandLink.MinimalFreehandLink freehandLink) {
        if (freehandLink != null && freehandLink != ProtobufFreehandLink.MinimalFreehandLink.getDefaultInstance()) {
            CotDetail freehandLinkDetail = new CotDetail(KEY_LINK);

            ByteString freehandLine = freehandLink.getLine();
            if (freehandLine != ProtobufFreehandLink.MinimalFreehandLink.getDefaultInstance().getLine()) {
                try {
                    freehandLinkDetail.setAttribute(KEY_LINE, GzipHelper.decompress(freehandLine.toByteArray()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            cotDetail.addChild(freehandLinkDetail);
        }
    }
}