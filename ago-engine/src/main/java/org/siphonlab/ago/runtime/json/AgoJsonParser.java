package org.siphonlab.ago.runtime.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import org.siphonlab.ago.AgoClass;
import org.siphonlab.ago.CallFrame;

public class AgoJsonParser extends JsonParserDelegate {
    protected final JsonParser parser;
    protected final AgoClass initialClass;
    private final CallFrame<?> callFrame;
    private final boolean serializeSlots;

    public AgoJsonParser(JsonParser parser, AgoClass initialClass, CallFrame<?> callFrame, boolean serializeSlots) {
        super(parser);
        this.parser = parser;
        this.initialClass = initialClass;
        this.callFrame = callFrame;
        this.serializeSlots = serializeSlots;
    }

    public boolean isSerializeSlots() {
        return serializeSlots;
    }

    public JsonParser getParser() {
        return parser;
    }

    public AgoClass getInitialClass() {
        return initialClass;
    }

    public CallFrame<?> getCallFrame() {
        return callFrame;
    }
}
