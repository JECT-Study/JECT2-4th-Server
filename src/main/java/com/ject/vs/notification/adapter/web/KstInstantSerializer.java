package com.ject.vs.notification.adapter.web;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;

public class KstInstantSerializer extends JsonSerializer<Instant> {
    private static final ZoneOffset KST = ZoneOffset.ofHours(9);

    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeString(value.atOffset(KST).toString());
    }
}
