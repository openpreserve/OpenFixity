package org.openpreservation.fixity.apps.server.exceptions;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface RFC7807Details extends Serializable {
    @JsonProperty("type")
    public String getType();
    @JsonProperty("title")
    public String getTitle();
    @JsonProperty("status")
    public int getStatus();
    @JsonProperty("detail")
    public String getDetail();
    @JsonProperty("instance")
    public String getInstance();
    @JsonProperty("traceId")
    public String getTraceId();

    static class DefaultRFC7807Details implements RFC7807Details {
        private final String type;
        private final String title;
        private final int status;
        private final String detail;
        private final String instance;
        private final String traceId;

        public DefaultRFC7807Details(String type, String title, int status, String detail, String instance, String traceId) {
            this.type = type;
            this.title = title;
            this.status = status;
            this.detail = detail;
            this.instance = instance;
            this.traceId = traceId;
        }

        @Override
        public String getType() {
            return type;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public String getDetail() {
            return detail;
        }

        @Override
        public String getInstance() {
            return instance;
        }

        @Override
        public String getTraceId() {
            return traceId;
        }
    }

    public static RFC7807DetailsBuilder of() {
        return RFC7807DetailsBuilder.builder();
    }

    public static RFC7807DetailsBuilder builderOfStatus(final int status) {
        return RFC7807DetailsBuilder.builder().withStatus(status);
    }

    public static class RFC7807DetailsBuilder {
        private String type;
        private String title;
        private int status;
        private String detail;
        private String instance;
        private String traceId;

        private RFC7807DetailsBuilder() {}

        static RFC7807DetailsBuilder builder() {
            return new RFC7807DetailsBuilder();
        }

        public RFC7807DetailsBuilder withType(String type) {
            this.type = type;
            return this;
        }

        public RFC7807DetailsBuilder withTitle(String title) {
            this.title = title;
            return this;
        }

        public RFC7807DetailsBuilder withStatus(int status) {
            this.status = status;
            return this;
        }

        public RFC7807DetailsBuilder withDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public RFC7807DetailsBuilder withInstance(String instance) {
            this.instance = instance;
            return this;
        }

        public RFC7807DetailsBuilder withTraceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public RFC7807Details build() {
            return new DefaultRFC7807Details(type, title, status, detail, instance, traceId);
        }
    }
}
