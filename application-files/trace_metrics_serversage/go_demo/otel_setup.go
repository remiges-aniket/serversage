package main

import (
	"context"
	"fmt"
	"time"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlpmetric/otlpmetricgrpc"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracegrpc"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	"go.opentelemetry.io/otel/sdk/trace"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// Initialize a gRPC connection to be used by both the tracer and meter
// providers.
func initConn() (*grpc.ClientConn, error) {
	// It connects the OpenTelemetry Collector through local gRPC connection.
	conn, err := grpc.NewClient(otel_grpc_endpoint,
		// Note the use of insecure transport here. TLS is recommended in production.
		grpc.WithTransportCredentials(insecure.NewCredentials()),
	)
	if err != nil {
		return nil, fmt.Errorf("failed to create gRPC connection to collector: %w", err)
	}

	return conn, err
}

func initResource() *resource.Resource {
	initResourcesOnce.Do(func() {
		extraResources, _ := resource.New(
			context.Background(),
			resource.WithOS(),
			resource.WithProcess(),
			resource.WithContainer(),
			resource.WithHost(),
			resource.WithTelemetrySDK(),
			resource.WithAttributes(
				service_name,
			),
		)
		rsorce, _ = resource.Merge(
			resource.Default(),
			extraResources,
		)
	})
	return rsorce
}

// Initializes an OTLP exporter, and configures the corresponding trace provider.
func initTracerProvider(ctx context.Context, conn *grpc.ClientConn) (func(context.Context) error, error) {
	// Set up a trace exporter
	traceExporter, err := otlptracegrpc.New(ctx, otlptracegrpc.WithGRPCConn(conn))
	if err != nil {
		return nil, fmt.Errorf("failed to create trace exporter: %w", err)
	}

	// Register the trace exporter with a TracerProvider, using a batch
	// span processor to aggregate spans before export.
	bsp := trace.NewBatchSpanProcessor(traceExporter)
	tracerProvider := trace.NewTracerProvider(
		trace.WithSampler(trace.AlwaysSample()),
		trace.WithResource(initResource()),
		trace.WithSpanProcessor(bsp),
	)
	otel.SetTracerProvider(tracerProvider)

	// Set global propagator to tracecontext (the default is no-op).
	otel.SetTextMapPropagator(propagation.TraceContext{})

	// Shutdown will flush any remaining spans and shut down the exporter.
	return tracerProvider.Shutdown, nil
}

// Initializes an OTLP exporter, and configures the corresponding meter provider.
func initMeterProvider(ctx context.Context, conn *grpc.ClientConn) (func(context.Context) error, error) {
	metricExporter, err := otlpmetricgrpc.New(ctx, otlpmetricgrpc.WithGRPCConn(conn))
	if err != nil {
		return nil, fmt.Errorf("failed to create metrics exporter: %w", err)
	}

	meterProvider := metric.NewMeterProvider(
		metric.WithReader(metric.NewPeriodicReader(metricExporter,
			// Default is 1m. Set to 3s for demonstrative purposes.
			metric.WithInterval(3*time.Second))),
		metric.WithResource(initResource()),
	)
	otel.SetMeterProvider(meterProvider)

	return meterProvider.Shutdown, nil
}

// ---------------------- Work Inprogress Below --------------------------------------
// var meterProvider *metric.MeterProvider

// func init() {
// 	// Set up OpenTelemetry metrics and Prometheus exporter
// 	exporter, err := prometheus.New()
// 	if err != nil {
// 		log.Fatalf("failed to create Prometheus exporter: %v", err)
// 	}

// 	// Set the OpenTelemetry meter provider with the Prometheus exporter
// 	meterProvider = metric.NewMeterProvider(metric.WithReader(exporter))

// 	// Register the meter provider globally
// 	otel.SetMeterProvider(meterProvider)
// }

// func recordMetrics() {
// 	// Get the meter from the global meter provider
// 	meter := meterProvider.Meter("go_demo_app")

// 	// Create a counter to track the request counts
// 	requestCount := meter.Int64Counter("http_server_requests_total", metric.WithDescription("Total HTTP requests"))

// 	// Create a histogram to track the request duration
// 	durationHistogram := meter.NewFloat64Histogram("http_server_request_duration_seconds", metric.WithDescription("Duration of HTTP requests"))

// 	// Register an HTTP handler for Prometheus scraping
// 	http.Handle("/metrics", promhttp.Handler())

// 	// Wrap the handler to record metrics for each request
// 	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
// 		// Record request count
// 		requestCount.Add(context.Background(), 1, attribute.String("status", "200"))

// 		// Start a timer to measure the duration of the request
// 		start := time.Now()

// 		// Simulate a simple response
// 		time.Sleep(100 * time.Millisecond)

// 		// Record the duration of the request
// 		durationHistogram.Record(context.Background(), time.Since(start).Seconds(), attribute.String("method", r.Method))

// 		// Send a simple response
// 		w.Write([]byte("Hello, world!"))
// 	})

// 	// Start the HTTP server to serve metrics and handle requests
// 	log.Fatal(http.ListenAndServe(":8080", nil))
// }
