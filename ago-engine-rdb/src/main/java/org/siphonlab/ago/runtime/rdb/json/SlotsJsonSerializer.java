package org.siphonlab.ago.runtime.rdb.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.siphonlab.ago.*;
import org.siphonlab.ago.runtime.AgoArrayInstance;
import org.siphonlab.ago.runtime.json.AgoJsonConfig;
import org.siphonlab.ago.runtime.json.AgoJsonGenerator;

import java.io.IOException;
import java.util.ArrayList;

import static org.siphonlab.ago.TypeCode.*;
import static org.siphonlab.ago.TypeCode.BOOLEAN_VALUE;
import static org.siphonlab.ago.TypeCode.BYTE_VALUE;
import static org.siphonlab.ago.TypeCode.CHAR_VALUE;
import static org.siphonlab.ago.TypeCode.CLASS_REF_VALUE;
import static org.siphonlab.ago.TypeCode.DOUBLE_VALUE;
import static org.siphonlab.ago.TypeCode.NULL_VALUE;
import static org.siphonlab.ago.TypeCode.OBJECT_VALUE;
import static org.siphonlab.ago.TypeCode.SHORT_VALUE;
import static org.siphonlab.ago.TypeCode.STRING_VALUE;
import static org.siphonlab.ago.runtime.json.InstanceJsonSerializer.writeSlots;

// bind attribute `slots_class` to transfer SlotDefs
// slots_instance is optional
public class SlotsJsonSerializer extends JsonSerializer<Slots> {

    private final AgoEngine agoEngine;

    public SlotsJsonSerializer(AgoEngine agoEngine) {
        this.agoEngine = agoEngine;
    }


    @Override
    public void serialize(Slots slots, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // now we are already here to stringify slots, therefore, it must be not box types
        // and shouldn't write object as reference
        // and not an array too, array is a slots type too
        // and not an AgoClass
        AgoClass agoClass = (AgoClass) serializers.getAttribute("slots_class");
        var instance = (Instance<?>) serializers.getAttribute("slots_instance");

        assert !(instance instanceof AgoArrayInstance);
        assert !(instance instanceof AgoClass);

        if (agoClass == null && instance != null) {
            agoClass = instance.getAgoClass();
        }

        AgoSlotDef[] slotDefs = agoClass.getSlotDefs();

        writeSlots(agoEngine, (AgoJsonGenerator) gen, slotDefs,
                new AgoJsonConfig(AgoJsonConfig.WriteTypeMode.Always, true, AgoJsonConfig.ObjectAsReferenceMode.Always, true),
                slots);
    }
}
