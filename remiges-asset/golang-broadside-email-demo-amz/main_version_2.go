package main

// import (
// 	"context"
// 	"encoding/json"
// 	"fmt"
// 	"log/slog"
// 	"math/rand"
// 	"net"
// 	"net/http"
// 	"os"
// 	"time"

// 	"github.com/prometheus/client_golang/prometheus/promhttp"
// 	"go.opentelemetry.io/otel"
// 	"go.opentelemetry.io/otel/attribute"
// 	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
// 	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
// 	"go.opentelemetry.io/otel/exporters/prometheus"
// 	"go.opentelemetry.io/otel/metric"
// 	"go.opentelemetry.io/otel/propagation"

// 	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
// 	"go.opentelemetry.io/otel/sdk/resource"
// 	sdktrace "go.opentelemetry.io/otel/sdk/trace"
// 	semconv "go.opentelemetry.io/otel/semconv/v1.34.0"
// 	"go.opentelemetry.io/otel/trace"
// )

// // Constants for configuration
// const (
// 	ServiceName    = "broadside-email-service"
// 	ServiceVersion = "1.0.0"
// 	AppPort        = ":8088"
// 	MeterName      = "bs.email.meter"
// )

// // Attribute keys
// var (
// 	DCSKey      = attribute.Key("dcs")
// 	StateKey    = attribute.Key("state")
// 	ProviderKey = attribute.Key("provider")
// 	StatusKey   = attribute.Key("status")
// 	RegionKey   = attribute.Key("region")
// )

// // EmailStatus represents email delivery status
// type EmailStatus string

// const (
// 	StatusInTransit EmailStatus = "in_transit"
// 	StatusSent      EmailStatus = "sent"
// 	StatusBounced   EmailStatus = "bounced"
// 	StatusRejected  EmailStatus = "rejected"
// )

// // EmailEvent represents an email processing event
// type EmailEvent struct {
// 	ID        string      `json:"id"`
// 	DCS       string      `json:"dcs"`
// 	State     int         `json:"state"`
// 	Region    string      `json:"region"`
// 	Provider  string      `json:"provider"`
// 	Status    EmailStatus `json:"status"`
// 	Count     int64       `json:"count"`
// 	Timestamp time.Time   `json:"timestamp"`
// }

// // MetricsCollector handles all metrics collection
// type MetricsCollector struct {
// 	emailCounter    metric.Int64Counter
// 	emailGauge      metric.Int64UpDownCounter
// 	processingHist  metric.Float64Histogram
// 	statusGauge     metric.Int64UpDownCounter
// 	providerCounter metric.Int64Counter
// 	logger          *slog.Logger
// 	tracer          trace.Tracer
// }

// // EmailProcessor handles email processing logic
// type EmailProcessor struct {
// 	metrics   *MetricsCollector
// 	logger    *slog.Logger
// 	tracer    trace.Tracer
// 	counters  map[EmailStatus]int64
// 	baseRates map[EmailStatus]float64
// }

// // Configuration holds service configuration
// type Configuration struct {
// 	ServiceName     string
// 	ServiceInstance string
// 	DCSOptions      []string
// 	ProviderOptions []string
// 	Regions         []string
// }

// // NewConfiguration creates a new configuration
// func NewConfiguration() *Configuration {
// 	return &Configuration{
// 		ServiceName:     ServiceName,
// 		ServiceInstance: getOutboundIP(),
// 		DCSOptions:      []string{"dcs1", "lsp", "lsh"},
// 		ProviderOptions: []string{"gmail", "hotmail", "outlook", "rediffmail", "yahoo", "others"},
// 		Regions:         []string{"us-east-1", "us-west-2", "eu-west-1", "ap-south-1"},
// 	}
// }

// // NewMetricsCollector creates a new metrics collector
// func NewMetricsCollector(meter metric.Meter, logger *slog.Logger, tracer trace.Tracer) (*MetricsCollector, error) {
// 	emailCounter, err := meter.Int64Counter(
// 		"bs_email_total",
// 		metric.WithDescription("Total number of emails processed"),
// 		metric.WithUnit("1"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create email counter: %w", err)
// 	}

// 	emailGauge, err := meter.Int64UpDownCounter(
// 		"bs_email_current",
// 		metric.WithDescription("Current email count by status"),
// 		metric.WithUnit("1"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create email gauge: %w", err)
// 	}

// 	processingHist, err := meter.Float64Histogram(
// 		"bs_email_processing_duration_seconds",
// 		metric.WithDescription("Email processing duration in seconds"),
// 		metric.WithUnit("s"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create processing histogram: %w", err)
// 	}

// 	statusGauge, err := meter.Int64UpDownCounter(
// 		"bs_email_status_count",
// 		metric.WithDescription("Email count by status"),
// 		metric.WithUnit("1"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create status gauge: %w", err)
// 	}

// 	providerCounter, err := meter.Int64Counter(
// 		"bs_email_provider_total",
// 		metric.WithDescription("Total emails by provider"),
// 		metric.WithUnit("1"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create provider counter: %w", err)
// 	}

// 	return &MetricsCollector{
// 		emailCounter:    emailCounter,
// 		emailGauge:      emailGauge,
// 		processingHist:  processingHist,
// 		statusGauge:     statusGauge,
// 		providerCounter: providerCounter,
// 		logger:          logger,
// 		tracer:          tracer,
// 	}, nil
// }

// // RecordEmailProcessing records email processing metrics
// func (mc *MetricsCollector) RecordEmailProcessing(ctx context.Context, event EmailEvent, duration time.Duration) {
// 	ctx, span := mc.tracer.Start(ctx, "record_email_metrics")
// 	defer span.End()

// 	attrs := []attribute.KeyValue{
// 		semconv.ServiceNameKey.String(ServiceName),
// 		semconv.ServiceInstanceIDKey.String(event.DCS),
// 		DCSKey.String(event.DCS),
// 		StateKey.Int(event.State),
// 		ProviderKey.String(event.Provider),
// 		StatusKey.String(string(event.Status)),
// 		RegionKey.String(event.Region),
// 	}

// 	// Record counter metrics
// 	mc.emailCounter.Add(ctx, event.Count, metric.WithAttributes(attrs...))
// 	mc.providerCounter.Add(ctx, event.Count, metric.WithAttributes(
// 		ProviderKey.String(event.Provider),
// 		StatusKey.String(string(event.Status)),
// 	))

// 	// Record gauge metrics
// 	mc.emailGauge.Add(ctx, event.Count, metric.WithAttributes(attrs...))
// 	mc.statusGauge.Add(ctx, event.Count, metric.WithAttributes(
// 		StatusKey.String(string(event.Status)),
// 	))

// 	// Record histogram
// 	mc.processingHist.Record(ctx, duration.Seconds(), metric.WithAttributes(attrs...))

// 	span.SetAttributes(attrs...)
// 	span.SetAttributes(
// 		attribute.String("event.id", event.ID),
// 		attribute.Int64("event.count", event.Count),
// 	)

// 	mc.logger.InfoContext(ctx, "Email processed",
// 		slog.String("event_id", event.ID),
// 		slog.String("dcs", event.DCS),
// 		slog.Int("state", event.State),
// 		slog.String("provider", event.Provider),
// 		slog.String("status", string(event.Status)),
// 		slog.String("region", event.Region),
// 		slog.Int64("count", event.Count),
// 		slog.Float64("processing_duration_seconds", duration.Seconds()),
// 	)
// }

// // NewEmailProcessor creates a new email processor
// func NewEmailProcessor(metrics *MetricsCollector, logger *slog.Logger, tracer trace.Tracer) *EmailProcessor {
// 	return &EmailProcessor{
// 		metrics: metrics,
// 		logger:  logger,
// 		tracer:  tracer,
// 		counters: map[EmailStatus]int64{
// 			StatusInTransit: 0,
// 			StatusSent:      0,
// 			StatusBounced:   0,
// 			StatusRejected:  0,
// 		},
// 		baseRates: map[EmailStatus]float64{
// 			StatusSent:      0.80, // 80%
// 			StatusInTransit: 0.10, // 10%
// 			StatusBounced:   0.07, // 7%
// 			StatusRejected:  0.03, // 3%
// 		},
// 	}
// }

// // ProcessEmails simulates email processing with realistic distribution
// func (ep *EmailProcessor) ProcessEmails(ctx context.Context, config *Configuration) {
// 	ticker := time.NewTicker(2 * time.Second)
// 	defer ticker.Stop()

// 	for {
// 		select {
// 		case <-ctx.Done():
// 			return
// 		case <-ticker.C:
// 			ep.processEmailBatch(ctx, config)
// 		}
// 	}
// }

// func (ep *EmailProcessor) processEmailBatch(ctx context.Context, config *Configuration) {
// 	ctx, span := ep.tracer.Start(ctx, "process_email_batch")
// 	defer span.End()

// 	// Generate emails with realistic distribution
// 	totalEmails := 50 + rand.Intn(200) // 50-250 emails per batch
// 	span.SetAttributes(attribute.Int("batch.total_emails", totalEmails))

// 	for i := 0; i < totalEmails; i++ {
// 		event := ep.generateEmailEvent(config)
// 		startTime := time.Now()

// 		// Simulate processing time
// 		processingTime := time.Duration(50+rand.Intn(200)) * time.Millisecond
// 		time.Sleep(processingTime)

// 		ep.metrics.RecordEmailProcessing(ctx, event, time.Since(startTime))
// 		ep.counters[event.Status] += event.Count
// 	}

// 	ep.logger.InfoContext(ctx, "Email batch processed",
// 		slog.Int("total_emails", totalEmails),
// 		slog.Int64("sent", ep.counters[StatusSent]),
// 		slog.Int64("in_transit", ep.counters[StatusInTransit]),
// 		slog.Int64("bounced", ep.counters[StatusBounced]),
// 		slog.Int64("rejected", ep.counters[StatusRejected]),
// 	)
// }

// func (ep *EmailProcessor) generateEmailEvent(config *Configuration) EmailEvent {
// 	// Generate status based on realistic distribution
// 	rand_val := rand.Float64()
// 	var status EmailStatus

// 	switch {
// 	case rand_val < ep.baseRates[StatusSent]:
// 		status = StatusSent
// 	case rand_val < ep.baseRates[StatusSent]+ep.baseRates[StatusInTransit]:
// 		status = StatusInTransit
// 	case rand_val < ep.baseRates[StatusSent]+ep.baseRates[StatusInTransit]+ep.baseRates[StatusBounced]:
// 		status = StatusBounced
// 	default:
// 		status = StatusRejected
// 	}

// 	// Generate varying counts based on status
// 	var count int64
// 	switch status {
// 	case StatusSent:
// 		count = int64(80 + rand.Intn(320)) // 80-400
// 	case StatusInTransit:
// 		count = int64(10 + rand.Intn(40)) // 10-50
// 	case StatusBounced:
// 		count = int64(5 + rand.Intn(25)) // 5-30
// 	case StatusRejected:
// 		count = int64(1 + rand.Intn(10)) // 1-11
// 	}

// 	return EmailEvent{
// 		ID:        fmt.Sprintf("email_%d_%d", time.Now().UnixNano(), rand.Intn(10000)),
// 		DCS:       config.DCSOptions[rand.Intn(len(config.DCSOptions))],
// 		State:     rand.Intn(100),
// 		Region:    config.Regions[rand.Intn(len(config.Regions))],
// 		Provider:  config.ProviderOptions[rand.Intn(len(config.ProviderOptions))],
// 		Status:    status,
// 		Count:     count,
// 		Timestamp: time.Now(),
// 	}
// }

// // initResource creates an OpenTelemetry resource
// func initResource(serviceName, serviceInstance string) (*resource.Resource, error) {
// 	return resource.Merge(
// 		resource.Default(),
// 		resource.NewWithAttributes(
// 			semconv.SchemaURL,
// 			semconv.ServiceNameKey.String(serviceName),
// 			semconv.ServiceVersionKey.String(ServiceVersion),
// 			semconv.ServiceInstanceIDKey.String(serviceInstance),
// 			semconv.DeploymentEnvironmentName("development"),
// 		),
// 	)
// }

// // initTracing initializes OpenTelemetry tracing
// func initTracing(ctx context.Context, res *resource.Resource) (*sdktrace.TracerProvider, error) {
// 	traceExporter, err := otlptracegrpc.New(ctx,
// 		otlptracegrpc.WithInsecure(),
// 		otlptracegrpc.WithEndpoint("localhost:4317"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create trace exporter: %w", err)
// 	}

// 	tp := sdktrace.NewTracerProvider(
// 		sdktrace.WithBatcher(traceExporter),
// 		sdktrace.WithResource(res),
// 		sdktrace.WithSampler(sdktrace.AlwaysSample()),
// 	)

// 	otel.SetTracerProvider(tp)
// 	otel.SetTextMapPropagator(propagation.TraceContext{})

// 	return tp, nil
// }

// // initMetrics initializes OpenTelemetry metrics
// func initMetrics(ctx context.Context, res *resource.Resource) (*sdkmetric.MeterProvider, error) {
// 	// Prometheus exporter for metrics endpoint
// 	promExporter, err := prometheus.New()
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create prometheus exporter: %w", err)
// 	}

// 	// OTLP exporter for sending to collector
// 	otlpExporter, err := otlpmetricgrpc.New(ctx,
// 		otlpmetricgrpc.WithInsecure(),
// 		otlpmetricgrpc.WithEndpoint("localhost:4317"),
// 	)
// 	if err != nil {
// 		return nil, fmt.Errorf("failed to create OTLP exporter: %w", err)
// 	}

// 	mp := sdkmetric.NewMeterProvider(
// 		sdkmetric.WithResource(res),
// 		sdkmetric.WithReader(promExporter),
// 		sdkmetric.WithReader(sdkmetric.NewPeriodicReader(
// 			otlpExporter,
// 			sdkmetric.WithInterval(10*time.Second),
// 		)),
// 		sdkmetric.WithView(
// 			sdkmetric.NewView(
// 				sdkmetric.Instrument{Name: "bs_email_processing_duration_seconds"},
// 				sdkmetric.Stream{
// 					Aggregation: sdkmetric.AggregationExplicitBucketHistogram{
// 						Boundaries: []float64{0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0},
// 					},
// 				},
// 			),
// 		),
// 	)

// 	otel.SetMeterProvider(mp)
// 	return mp, nil
// }

// // initLogging initializes structured logging
// func initLogging() *slog.Logger {
// 	opts := &slog.HandlerOptions{
// 		Level: slog.LevelInfo,
// 		ReplaceAttr: func(groups []string, a slog.Attr) slog.Attr {
// 			if a.Key == slog.TimeKey {
// 				a.Value = slog.StringValue(time.Now().Format(time.RFC3339))
// 			}
// 			return a
// 		},
// 	}

// 	handler := slog.NewJSONHandler(os.Stdout, opts)
// 	return slog.New(handler)
// }

// // getOutboundIP gets the preferred outbound IP address
// func getOutboundIP() string {
// 	conn, err := net.Dial("udp", "8.8.8.8:80")
// 	if err != nil {
// 		return "unknown_ip"
// 	}
// 	defer conn.Close()

// 	localAddr := conn.LocalAddr().(*net.UDPAddr)
// 	return localAddr.IP.String()
// }

// // healthCheck provides a health check endpoint
// func healthCheck(w http.ResponseWriter, r *http.Request) {
// 	w.Header().Set("Content-Type", "application/json")
// 	json.NewEncoder(w).Encode(map[string]string{
// 		"status":    "healthy",
// 		"service":   ServiceName,
// 		"version":   ServiceVersion,
// 		"timestamp": time.Now().Format(time.RFC3339),
// 	})
// }

// func main() {
// 	ctx := context.Background()

// 	// Initialize logging
// 	logger := initLogging()
// 	logger.Info("Starting email service", slog.String("service", ServiceName))

// 	// Initialize configuration
// 	config := NewConfiguration()
// 	logger.Info("Configuration loaded",
// 		slog.String("instance", config.ServiceInstance),
// 		slog.Any("dcs_options", config.DCSOptions),
// 		slog.Any("providers", config.ProviderOptions),
// 	)

// 	// Initialize OpenTelemetry resource
// 	res, err := initResource(config.ServiceName, config.ServiceInstance)
// 	if err != nil {
// 		logger.Error("Failed to initialize resource", slog.Any("error", err))
// 		os.Exit(1)
// 	}

// 	// Initialize tracing
// 	tp, err := initTracing(ctx, res)
// 	if err != nil {
// 		logger.Error("Failed to initialize tracing", slog.Any("error", err))
// 		os.Exit(1)
// 	}
// 	defer func() {
// 		if err := tp.Shutdown(ctx); err != nil {
// 			logger.Error("Error shutting down tracer provider", slog.Any("error", err))
// 		}
// 	}()

// 	// Initialize metrics
// 	mp, err := initMetrics(ctx, res)
// 	if err != nil {
// 		logger.Error("Failed to initialize metrics", slog.Any("error", err))
// 		os.Exit(1)
// 	}
// 	defer func() {
// 		if err := mp.Shutdown(ctx); err != nil {
// 			logger.Error("Error shutting down meter provider", slog.Any("error", err))
// 		}
// 	}()

// 	// Get meter and tracer
// 	meter := otel.Meter(MeterName)
// 	tracer := otel.Tracer(ServiceName)

// 	// Initialize metrics collector
// 	metricsCollector, err := NewMetricsCollector(meter, logger, tracer)
// 	if err != nil {
// 		logger.Error("Failed to create metrics collector", slog.Any("error", err))
// 		os.Exit(1)
// 	}

// 	// Initialize email processor
// 	processor := NewEmailProcessor(metricsCollector, logger, tracer)

// 	// Start email processing
// 	go processor.ProcessEmails(ctx, config)

// 	// Set up HTTP routes
// 	http.Handle("/metrics", promhttp.Handler())
// 	http.HandleFunc("/health", healthCheck)

// 	logger.Info("Server starting", slog.String("port", AppPort))
// 	if err := http.ListenAndServe(AppPort, nil); err != nil {
// 		logger.Error("Server failed to start", slog.Any("error", err))
// 		os.Exit(1)
// 	}
// }
