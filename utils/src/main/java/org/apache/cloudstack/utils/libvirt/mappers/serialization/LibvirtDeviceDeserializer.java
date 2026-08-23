package org.apache.cloudstack.utils.libvirt.mappers.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.cloudstack.utils.libvirt.model.LibvirtDevice;
import org.apache.cloudstack.utils.libvirt.model.PciDevice;

import java.io.IOException;

public class LibvirtDeviceDeserializer extends JsonDeserializer<LibvirtDevice> {
    @Override
    public LibvirtDevice deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        String type = node.hasNonNull("deviceType") ? node.get("deviceType").asText() : null;

        if (type == null) {
            throw JsonMappingException.from(p, "Missing device type discriminator");
        }

        switch (type) {
            case "pci":
                return codec.treeToValue(node, PciDevice.class);
            default:
                throw JsonMappingException.from(p, "Unsupported LibvirtDevice type: " + type);
        }
    }
}
