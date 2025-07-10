package tech.remiges.serversage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LoggerProvider;
import io.opentelemetry.api.metrics.MeterProvider;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class OpenTelemetryConfig {

    @Value("${otel.service.name:serversage}")
    private String serviceName;

    @Value("${otel.service.version:1.0.0}")
    private String serviceVersion;

    @Value("${otel.deployment.environment:development}")
    private String deploymentEnvironment;

    @Value("${otel.exporter.otlp.endpoint:http://localhost:4317}")
    private String otlpEndpoint;

    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault()
                .merge(Resource.create(Attributes.of(
                        ResourceAttributes.SERVICE_NAME, serviceName,
                        ResourceAttributes.SERVICE_VERSION, serviceVersion,
                        ResourceAttributes.DEPLOYMENT_ENVIRONMENT, deploymentEnvironment)));

        // Configure Trace Provider
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(
                        OtlpGrpcSpanExporter.builder()
                                .setEndpoint(otlpEndpoint)
                                .build())
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .build())
                .setResource(resource)
                .build();

        // Configure Metric Provider
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(
                        OtlpGrpcMetricExporter.builder()
                                .setEndpoint(otlpEndpoint)
                                .build())
                        .setInterval(Duration.ofSeconds(10))
                        .build())
                .setResource(resource)
                .build();

        // Configure Log Provider
        SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(
                        OtlpGrpcLogRecordExporter.builder()
                                .setEndpoint(otlpEndpoint)
                                .build())
                        .setMaxExportBatchSize(512)
                        .setScheduleDelay(Duration.ofSeconds(1))
                        .build())
                .setResource(resource)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .build();

        // Initialize the OpenTelemetry Logback appender
        OpenTelemetryAppender.install(openTelemetry);

        return openTelemetry;
    }

    @Bean
    public TracerProvider tracerProvider(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracerProvider();
    }

    @Bean
    public MeterProvider meterProvider(OpenTelemetry openTelemetry) {
        return openTelemetry.getMeterProvider();
    }

    @Bean
    public LoggerProvider loggerProvider(OpenTelemetry openTelemetry) {
        return openTelemetry.getLogsBridge();
    }

    @Bean
    public ContextPropagators contextPropagators(OpenTelemetry openTelemetry) {
        return openTelemetry.getPropagators();
    }
}
