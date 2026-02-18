package org.openpreservation.fixity.apps.schedule;

import java.io.IOException;

import org.jspecify.annotations.NonNull;
import org.quartz.JobDetail;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JobDetailSerializer extends JsonSerializer<@NonNull JobDetail> {

    @Override
    public void serialize(@NonNull JobDetail value, JsonGenerator gen, SerializerProvider serialiser) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("name", value.getKey().getName());
        gen.writeStringField("group", value.getKey().getGroup());
        gen.writeStringField("jobClass", value.getJobClass().getName());
        gen.writeEndObject();
    }

}
