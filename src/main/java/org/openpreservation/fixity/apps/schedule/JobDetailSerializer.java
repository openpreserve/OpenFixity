/*
 * OpenFixity is an application for monitoring and reporting on the fixity of files.
 * Copyright (C) 2026 Open Preservation Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
