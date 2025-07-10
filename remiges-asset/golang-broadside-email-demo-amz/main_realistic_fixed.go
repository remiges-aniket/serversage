package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"math/rand"
	"net"
	"net/http"
	"os"
	"time"

	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/exporters/prometheus"
	"go.opentelemetry.io/otel/metric"
	"go.opentelemetry.io/otel/propagation"

	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.34.0"
	"go.opentelemetry.io/otel/trace"
)

// Constants for configuration
const (
	ServiceName    = "broadside-email-service"
	ServiceVersion = "1.0.0"
	AppPort        = ":8088"
	MeterName      = "bs.email.meter"
)

// Attribute keys
var (
	DCSKey      = attribute.Key("dcs")
	StateKey    = attribute.Key("state")
	ProviderKey = attribute.Key("provider")
	StatusKey   = attribute.Key("status")
	RegionKey   = attribute.Key("region")
)

// EmailStatus represents email delivery status
type EmailStatus string

const (
	StatusInTransit EmailStatus = "in_transit"
	StatusSent      EmailStatus = "sent"
	StatusBounced   EmailStatus = "bounced"
	StatusRejected  EmailStatus = "rejected"
)

// ProviderConfig holds provider-specific configuration
type ProviderConfig struct {
	Name        string
	Weight      float64 // Probability weight for selection
	BaseVolume  int     // Base volume multiplier
	StatusRates map[EmailStatus]float64
}

// EmailEvent represents an email processing event
type EmailEvent struct {
	ID        string      `json:"id"`
	DCS       string      `json:"dcs"`
	State     int         `json:"state"`
	Region    string      `json:"region"`
	Provider  string      `json:"provider"`
	Status    EmailStatus `json:"status"`
	Count     int64       `json:"count"`
	Timestamp time.Time   `json:"timestamp"`
}

// MetricsCollector handles all metrics collection
type MetricsCollector struct {
	emailCounter    metric.Int64Counter
	emailGauge      metric.Int64UpDownCounter
	processingHist  metric.Float64Histogram
	statusGauge     metric.Int64UpDownCounter
	providerCounter metric.Int64Counter
	logger          *slog.Logger
	tracer          trace.Tracer
}

// RealisticEmailProcessor handles email processing with realistic patterns
type RealisticEmailProcessor struct {
	metrics         *MetricsCollector
	logger          *slog.Logger
	tracer          trace.Tracer
	counters        map[EmailStatus]int64
	providerConfigs []ProviderConfig
	dcsOptions      []string
	regions         []string
	totalWeights    float64
}

// Configuration holds service configuration
type Configuration struct {
	ServiceName     string
	ServiceInstance string
}

// NewConfiguration creates a new configuration
func NewConfiguration() *Configuration {
	return &Configuration{
		ServiceName:     ServiceName,
		ServiceInstance: getOutboundIP(),
	}
}

// NewMetricsCollector creates a new metrics collector
func NewMetricsCollector(meter metric.Meter, logger *slog.Logger, tracer trace.Tracer) (*MetricsCollector, error) {
	emailCounter, err := meter.Int64Counter(
		"bs_email_total",
		metric.WithDescription("Total number of emails processed"),
		metric.WithUnit("1"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create email counter: %w", err)
	}

	emailGauge, err := meter.Int64UpDownCounter(
		"bs_email_current",
		metric.WithDescription("Current email count by status"),
		metric.WithUnit("1"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create email gauge: %w", err)
	}

	processingHist, err := meter.Float64Histogram(
		"bs_email_processing_duration_seconds",
		metric.WithDescription("Email processing duration in seconds"),
		metric.WithUnit("s"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create processing histogram: %w", err)
	}

	statusGauge, err := meter.Int64UpDownCounter(
		"bs_email_status_count",
		metric.WithDescription("Email count by status"),
		metric.WithUnit("1"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create status gauge: %w", err)
	}

	providerCounter, err := meter.Int64Counter(
		"bs_email_provider_total",
		metric.WithDescription("Total emails by provider"),
		metric.WithUnit("1"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create provider counter: %w", err)
	}

	return &MetricsCollector{
		emailCounter:    emailCounter,
		emailGauge:      emailGauge,
		processingHist:  processingHist,
		statusGauge:     statusGauge,
		providerCounter: providerCounter,
		logger:          logger,
		tracer:          tracer,
	}, nil
}

// RecordEmailProcessing records email processing metrics
func (mc *MetricsCollector) RecordEmailProcessing(ctx context.Context, event EmailEvent, duration time.Duration) {
	ctx, span := mc.tracer.Start(ctx, "record_email_metrics")
	defer span.End()

	attrs := []attribute.KeyValue{
		semconv.ServiceNameKey.String(ServiceName),
		semconv.ServiceInstanceIDKey.String(event.DCS),
		DCSKey.String(event.DCS),
		StateKey.Int(event.State),
		ProviderKey.String(event.Provider),
		StatusKey.String(string(event.Status)),
		RegionKey.String(event.Region),
	}

	// Record counter metrics
	mc.emailCounter.Add(ctx, event.Count, metric.WithAttributes(attrs...))
	mc.providerCounter.Add(ctx, event.Count, metric.WithAttributes(
		ProviderKey.String(event.Provider),
		StatusKey.String(string(event.Status)),
	))

	// Record gauge metrics
	mc.emailGauge.Add(ctx, event.Count, metric.WithAttributes(attrs...))
	mc.statusGauge.Add(ctx, event.Count, metric.WithAttributes(
		StatusKey.String(string(event.Status)),
	))

	// Record histogram
	mc.processingHist.Record(ctx, duration.Seconds(), metric.WithAttributes(attrs...))

	span.SetAttributes(attrs...)
	span.SetAttributes(
		attribute.String("event.id", event.ID),
		attribute.Int64("event.count", event.Count),
	)

	mc.logger.InfoContext(ctx, "Email processed",
		slog.String("event_id", event.ID),
		slog.String("dcs", event.DCS),
		slog.Int("state", event.State),
		slog.String("provider", event.Provider),
		slog.String("status", string(event.Status)),
		slog.String("region", event.Region),
		slog.Int64("count", event.Count),
		slog.Float64("processing_duration_seconds", duration.Seconds()),
	)
}

// NewRealisticEmailProcessor creates a new realistic email processor
func NewRealisticEmailProcessor(metrics *MetricsCollector, logger *slog.Logger, tracer trace.Tracer) *RealisticEmailProcessor {
	// Realistic provider configurations based on market share and your requirements
	providerConfigs := []ProviderConfig{
		{
			Name:       "gmail",
			Weight:     45.0, // 45% of all emails (dominant)
			BaseVolume: 100,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.85, // Gmail has good delivery rates
				StatusInTransit: 0.08,
				StatusBounced:   0.05,
				StatusRejected:  0.02,
			},
		},
		{
			Name:       "hotmail",
			Weight:     20.0, // 20% of all emails
			BaseVolume: 60,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.82,
				StatusInTransit: 0.10,
				StatusBounced:   0.06,
				StatusRejected:  0.02,
			},
		},
		{
			Name:       "rediff",
			Weight:     12.0, // 12% of all emails
			BaseVolume: 40,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.78,
				StatusInTransit: 0.12,
				StatusBounced:   0.07,
				StatusRejected:  0.03,
			},
		},
		{
			Name:       "yahoo",
			Weight:     10.0, // 10% of all emails
			BaseVolume: 35,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.80,
				StatusInTransit: 0.11,
				StatusBounced:   0.06,
				StatusRejected:  0.03,
			},
		},
		{
			Name:       "outlook",
			Weight:     8.0, // 8% of all emails
			BaseVolume: 30,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.83,
				StatusInTransit: 0.09,
				StatusBounced:   0.05,
				StatusRejected:  0.03,
			},
		},
		{
			Name:       "others",
			Weight:     5.0, // 5% of all emails (smallest)
			BaseVolume: 20,
			StatusRates: map[EmailStatus]float64{
				StatusSent:      0.75, // Others might have lower delivery rates
				StatusInTransit: 0.15,
				StatusBounced:   0.08,
				StatusRejected:  0.02,
			},
		},
	}

	// Calculate total weights for weighted random selection
	totalWeights := 0.0
	for _, config := range providerConfigs {
		totalWeights += config.Weight
	}

	return &RealisticEmailProcessor{
		metrics:         metrics,
		logger:          logger,
		tracer:          tracer,
		providerConfigs: providerConfigs,
		totalWeights:    totalWeights,
		dcsOptions:      []string{"dcs1", "lsp", "lsh"},
		regions:         []string{"us-east-1", "us-west-2", "eu-west-1", "ap-south-1"},
		counters: map[EmailStatus]int64{
			StatusInTransit: 0,
			StatusSent:      0,
			StatusBounced:   0,
			StatusRejected:  0,
		},
	}
}

// ProcessEmails simulates realistic email processing with variable timing
func (rep *RealisticEmailProcessor) ProcessEmails(ctx context.Context) {
	go func() {
		for {
			select {
			case <-ctx.Done():
				return
			default:
				// Random interval between 1-8 seconds to make it more realistic
				interval := time.Duration(1000+rand.Intn(7000)) * time.Millisecond
				time.Sleep(interval)
				
				rep.processRealisticEmailBatch(ctx)
			}
		}
	}()
}

// processRealisticEmailBatch processes a batch of emails with realistic distribution
func (rep *RealisticEmailProcessor) processRealisticEmailBatch(ctx context.Context) {
	ctx, span := rep.tracer.Start(ctx, "process_realistic_email_batch")
	defer span.End()

	// Generate realistic batch size based on time of day simulation
	hour := time.Now().Hour()
	batchMultiplier := rep.getTimeBasedMultiplier(hour)
	
	// Base batch size with time-based variation
	baseBatchSize := 20 + rand.Intn(80) // 20-100 base emails
	totalEmails := int(float64(baseBatchSize) * batchMultiplier)
	
	span.SetAttributes(
		attribute.Int("batch.total_emails", totalEmails),
		attribute.Int("batch.hour", hour),
		attribute.Float64("batch.multiplier", batchMultiplier),
	)

	batchCounters := make(map[string]map[EmailStatus]int64)

	for i := 0; i < totalEmails; i++ {
		// Select provider based on realistic weights
		provider := rep.selectWeightedProvider()
		providerConfig := rep.getProviderConfig(provider)
		
		// Generate event for this provider
		event := rep.generateRealisticEmailEvent(providerConfig)
		
		// Track batch statistics
		if batchCounters[provider] == nil {
			batchCounters[provider] = make(map[EmailStatus]int64)
		}
		batchCounters[provider][event.Status] += event.Count

		startTime := time.Now()
		
		// Simulate realistic processing time based on provider
		processingTime := rep.getProcessingTime(provider)
		time.Sleep(processingTime)

		rep.metrics.RecordEmailProcessing(ctx, event, time.Since(startTime))
		rep.counters[event.Status] += event.Count
	}

	// Log batch summary
	rep.logBatchSummary(ctx, totalEmails, batchCounters)
}

// selectWeightedProvider selects a provider based on realistic weights
func (rep *RealisticEmailProcessor) selectWeightedProvider() string {
	randomValue := rand.Float64() * rep.totalWeights
	currentWeight := 0.0
	
	for _, config := range rep.providerConfigs {
		currentWeight += config.Weight
		if randomValue <= currentWeight {
			return config.Name
		}
	}
	
	// Fallback to last provider
	return rep.providerConfigs[len(rep.providerConfigs)-1].Name
}

// getProviderConfig returns the configuration for a given provider
func (rep *RealisticEmailProcessor) getProviderConfig(provider string) ProviderConfig {
	for _, config := range rep.providerConfigs {
		if config.Name == provider {
			return config
		}
	}
	// Fallback to first provider config
	return rep.providerConfigs[0]
}

// generateRealisticEmailEvent generates an email event with realistic attributes
func (rep *RealisticEmailProcessor) generateRealisticEmailEvent(providerConfig ProviderConfig) EmailEvent {
	// Generate status based on provider-specific rates
	status := rep.selectStatusForProvider(providerConfig)
	
	// Generate realistic count based on provider and status
	count := rep.generateRealisticCount(providerConfig, status)
	
	// Add some randomness to state distribution
	state := rep.generateRealisticState()

	return EmailEvent{
		ID:        fmt.Sprintf("email_%d_%d", time.Now().UnixNano(), rand.Intn(10000)),
		DCS:       selectWeightedDCS(),
		State:     state,
		Region:    selectWeightedRegion(),
		Provider:  providerConfig.Name,
		Status:    status,
		Count:     count,
		Timestamp: time.Now(),
	}
}

// selectStatusForProvider selects status based on provider-specific rates
func (rep *RealisticEmailProcessor) selectStatusForProvider(config ProviderConfig) EmailStatus {
	randVal := rand.Float64()
	cumulative := 0.0
	
	for status, rate := range config.StatusRates {
		cumulative += rate
		if randVal <= cumulative {
			return status
		}
	}
	
	// Fallback
	return StatusSent
}

// generateRealisticCount generates realistic email count based on provider and status
func (rep *RealisticEmailProcessor) generateRealisticCount(config ProviderConfig, status EmailStatus) int64 {
	baseCount := config.BaseVolume
	
	// Adjust count based on status
	switch status {
	case StatusSent:
		return int64(baseCount + rand.Intn(baseCount*2)) // High volume for sent
	case StatusInTransit:
		return int64(baseCount/4 + rand.Intn(baseCount/2)) // Medium volume
	case StatusBounced:
		return int64(baseCount/8 + rand.Intn(baseCount/4)) // Lower volume
	case StatusRejected:
		return int64(1 + rand.Intn(baseCount/8)) // Lowest volume
	default:
		return int64(1 + rand.Intn(10))
	}
}

// generateRealisticState generates realistic state distribution
func (rep *RealisticEmailProcessor) generateRealisticState() int {
	// Create realistic state distribution (not uniform)
	// Most emails in certain states, fewer in others
	weights := []float64{0.3, 0.25, 0.2, 0.15, 0.1} // Top 5 states get most traffic
	
	randVal := rand.Float64()
	cumulative := 0.0
	
	for i, weight := range weights {
		cumulative += weight
		if randVal <= cumulative {
			return i * 20 // States 0, 20, 40, 60, 80
		}
	}
	
	// Remaining states (less common)
	return 85 + rand.Intn(15) // States 85-99
}

// getTimeBasedMultiplier returns a multiplier based on hour to simulate realistic traffic patterns
func (rep *RealisticEmailProcessor) getTimeBasedMultiplier(hour int) float64 {
	// Simulate realistic email traffic patterns throughout the day
	// Peak hours: 9-11 AM and 2-4 PM
	// Low hours: 12-6 AM
	
	switch {
	case hour >= 0 && hour < 6:   // Night: very low traffic
		return 0.2 + rand.Float64()*0.3 // 0.2-0.5x
	case hour >= 6 && hour < 9:   // Morning: increasing traffic
		return 0.6 + rand.Float64()*0.4 // 0.6-1.0x
	case hour >= 9 && hour < 12:  // Peak morning: high traffic
		return 1.2 + rand.Float64()*0.8 // 1.2-2.0x
	case hour >= 12 && hour < 14: // Lunch: moderate traffic
		return 0.8 + rand.Float64()*0.4 // 0.8-1.2x
	case hour >= 14 && hour < 17: // Peak afternoon: high traffic
		return 1.1 + rand.Float64()*0.7 // 1.1-1.8x
	case hour >= 17 && hour < 20: // Evening: moderate traffic
		return 0.7 + rand.Float64()*0.5 // 0.7-1.2x
	default:                      // Night: low traffic
		return 0.3 + rand.Float64()*0.4 // 0.3-0.7x
	}
}

// getProcessingTime returns realistic processing time based on provider
func (rep *RealisticEmailProcessor) getProcessingTime(provider string) time.Duration {
	// Different providers might have different processing characteristics
	baseTime := 50 // milliseconds
	
	switch provider {
	case "gmail":
		return time.Duration(baseTime+rand.Intn(100)) * time.Millisecond
	case "hotmail", "outlook":
		return time.Duration(baseTime+rand.Intn(150)) * time.Millisecond
	case "yahoo":
		return time.Duration(baseTime+rand.Intn(120)) * time.Millisecond
	case "rediff":
		return time.Duration(baseTime+rand.Intn(200)) * time.Millisecond
	default: // others
		return time.Duration(baseTime+rand.Intn(300)) * time.Millisecond
	}
}

// logBatchSummary logs a summary of the processed batch
func (rep *RealisticEmailProcessor) logBatchSummary(ctx context.Context, totalEmails int, batchCounters map[string]map[EmailStatus]int64) {
	rep.logger.InfoContext(ctx, "Realistic email batch processed",
		slog.Int("total_emails", totalEmails),
		slog.Int64("total_sent", rep.counters[StatusSent]),
		slog.Int64("total_in_transit", rep.counters[StatusInTransit]),
		slog.Int64("total_bounced", rep.counters[StatusBounced]),
		slog.Int64("total_rejected", rep.counters[StatusRejected]),
		slog.Any("batch_by_provider", batchCounters),
	)
}

// initResource creates an OpenTelemetry resource
func initResource(serviceName, serviceInstance string) (*resource.Resource, error) {
	return resource.Merge(
		resource.Default(),
		resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceNameKey.String(serviceName),
			semconv.ServiceVersionKey.String(ServiceVersion),
			semconv.ServiceInstanceIDKey.String(serviceInstance),
			semconv.DeploymentEnvironmentName("development"),
		),
	)
}

// initTracing initializes OpenTelemetry tracing
func initTracing(ctx context.Context, res *resource.Resource) (*sdktrace.TracerProvider, error) {
	traceExporter, err := otlptracegrpc.New(ctx,
		otlptracegrpc.WithInsecure(),
		otlptracegrpc.WithEndpoint("localhost:4317"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create trace exporter: %w", err)
	}

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(traceExporter),
		sdktrace.WithResource(res),
		sdktrace.WithSampler(sdktrace.AlwaysSample()),
	)

	otel.SetTracerProvider(tp)
	otel.SetTextMapPropagator(propagation.TraceContext{})

	return tp, nil
}

// initMetrics initializes OpenTelemetry metrics
func initMetrics(ctx context.Context, res *resource.Resource) (*sdkmetric.MeterProvider, error) {
	// Prometheus exporter for metrics endpoint
	promExporter, err := prometheus.New()
	if err != nil {
		return nil, fmt.Errorf("failed to create prometheus exporter: %w", err)
	}

	// OTLP exporter for sending to collector
	otlpExporter, err := otlpmetricgrpc.New(ctx,
		otlpmetricgrpc.WithInsecure(),
		otlpmetricgrpc.WithEndpoint("localhost:4317"),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create OTLP exporter: %w", err)
	}

	mp := sdkmetric.NewMeterProvider(
		sdkmetric.WithResource(res),
		sdkmetric.WithReader(promExporter),
		sdkmetric.WithReader(sdkmetric.NewPeriodicReader(
			otlpExporter,
			sdkmetric.WithInterval(10*time.Second),
		)),
		sdkmetric.WithView(
			sdkmetric.NewView(
				sdkmetric.Instrument{Name: "bs_email_processing_duration_seconds"},
				sdkmetric.Stream{
					Aggregation: sdkmetric.AggregationExplicitBucketHistogram{
						Boundaries: []float64{0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0},
					},
				},
			),
		),
	)

	otel.SetMeterProvider(mp)
	return mp, nil
}

// initLogging initializes structured logging
func initLogging() *slog.Logger {
	opts := &slog.HandlerOptions{
		Level: slog.LevelInfo,
		ReplaceAttr: func(groups []string, a slog.Attr) slog.Attr {
			if a.Key == slog.TimeKey {
				a.Value = slog.StringValue(time.Now().Format(time.RFC3339))
			}
			return a
		},
	}

	handler := slog.NewJSONHandler(os.Stdout, opts)
	return slog.New(handler)
}

// getOutboundIP gets the preferred outbound IP address
func getOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80")
	if err != nil {
		return "unknown_ip"
	}
	defer conn.Close()

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

// healthCheck provides a health check endpoint
func healthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{
		"status":    "healthy",
		"service":   ServiceName,
		"version":   ServiceVersion,
		"timestamp": time.Now().Format(time.RFC3339),
	})
}

// selectWeightedDCS selects DCS with realistic weights (NON-UNIFORM)
func selectWeightedDCS() string {
	r := rand.Float64()
	if r < 0.5 { return "dcs1" }      // 50% - Primary DCS (dominant)
	if r < 0.8 { return "lsp" }       // 30% - Secondary DCS
	return "lsh"                      // 20% - Tertiary DCS (smallest)
}

// selectWeightedRegion selects region with realistic weights (NON-UNIFORM)
func selectWeightedRegion() string {
	r := rand.Float64()
	if r < 0.4 { return "us-east-1" }  // 40% - Primary region (East Coast dominant)
	if r < 0.65 { return "us-west-2" } // 25% - Secondary region (West Coast)
	if r < 0.85 { return "eu-west-1" } // 20% - European region
	return "ap-south-1"                // 15% - Asia Pacific (smallest)
}

func main() {
	ctx := context.Background()

	// Seed random number generator for realistic randomness
	rand.Seed(time.Now().UnixNano())

	// Initialize logging
	logger := initLogging()
	logger.Info("Starting realistic email service", slog.String("service", ServiceName))

	// Initialize configuration
	config := NewConfiguration()
	logger.Info("Configuration loaded",
		slog.String("instance", config.ServiceInstance),
	)

	// Initialize OpenTelemetry resource
	res, err := initResource(config.ServiceName, config.ServiceInstance)
	if err != nil {
		logger.Error("Failed to initialize resource", slog.Any("error", err))
		os.Exit(1)
	}

	// Initialize tracing
	tp, err := initTracing(ctx, res)
	if err != nil {
		logger.Error("Failed to initialize tracing", slog.Any("error", err))
		os.Exit(1)
	}
	defer func() {
		if err := tp.Shutdown(ctx); err != nil {
			logger.Error("Error shutting down tracer provider", slog.Any("error", err))
		}
	}()

	// Initialize metrics
	mp, err := initMetrics(ctx, res)
	if err != nil {
		logger.Error("Failed to initialize metrics", slog.Any("error", err))
		os.Exit(1)
	}
	defer func() {
		if err := mp.Shutdown(ctx); err != nil {
			logger.Error("Error shutting down meter provider", slog.Any("error", err))
		}
	}()

	// Get meter and tracer
	meter := otel.Meter(MeterName)
	tracer := otel.Tracer(ServiceName)

	// Initialize metrics collector
	metricsCollector, err := NewMetricsCollector(meter, logger, tracer)
	if err != nil {
		logger.Error("Failed to create metrics collector", slog.Any("error", err))
		os.Exit(1)
	}

	// Initialize realistic email processor
	processor := NewRealisticEmailProcessor(metricsCollector, logger, tracer)

	// Log provider configuration for verification
	logger.Info("Provider configuration loaded",
		slog.Any("providers", map[string]interface{}{
			"gmail":   "45% weight, high volume",
			"hotmail": "20% weight, medium-high volume", 
			"rediff":  "12% weight, medium volume",
			"yahoo":   "10% weight, medium volume",
			"outlook": "8% weight, medium-low volume",
			"others":  "5% weight, low volume",
		}),
	)

	// Start realistic email processing
	processor.ProcessEmails(ctx)

	// Set up HTTP routes
	http.Handle("/metrics", promhttp.Handler())
	http.HandleFunc("/health", healthCheck)

	logger.Info("Server starting with realistic email patterns", slog.String("port", AppPort))
	if err := http.ListenAndServe(AppPort, nil); err != nil {
		logger.Error("Server failed to start", slog.Any("error", err))
		os.Exit(1)
	}
}

