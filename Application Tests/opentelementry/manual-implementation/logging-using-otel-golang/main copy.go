package main

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"github.com/mbyd916/context/userip"
	"go.opentelemetry.io/contrib/bridges/otelslog"
	"go.opentelemetry.io/otel/exporters/otlp/otlplog/otlploghttp"
	"go.opentelemetry.io/otel/log/global"
	logsdk "go.opentelemetry.io/otel/sdk/log"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.21.0"
)

const (
	APP_NAME = "my-otel-test-app"
)

func main() {

	ctx := context.Background()

	// Initialize OpenTelemetry
	logExporter, err := otlploghttp.New(ctx)
	if err != nil {
		fmt.Println("Failed to create log exporter: %v", err)
	}
	clientIP, _ := userip.FromContext(ctx)
	// Configure resource
	res, _ := resource.Merge(
		resource.Default(),
		resource.NewWithAttributes(
			semconv.SchemaURL,
			semconv.ServiceName("myapp	"),
			semconv.HostName("server-01"),
		),
	)
	// Create logger provider
	loggerProvider := logsdk.NewLoggerProvider(
		logsdk.WithResource(res),
		logsdk.WithProcessor(logsdk.NewBatchProcessor(logExporter)),
	)
	defer loggerProvider.Shutdown(ctx)
	// set it to global
	global.SetLoggerProvider(loggerProvider)

	// Create bridge to slog
	otelLogger := otelslog.NewLogger("otel-logger-" + APP_NAME)

	//----------------------------------------------------------------------------
	// Example 1: Basic application log
	otelLogger.InfoContext(ctx, "User login",
		slog.String("user.id", "123"),
		slog.String("auth.method", "oauth"),
	)

	// Example 2: HTTP access log
	otelLogger.InfoContext(ctx, "HTTP request",
		slog.String("network.peer.address", clientIP.String()),
		slog.String("http.method", "GET"),
		slog.Int("http.status_code", 200),
		slog.String("url.path", "/api/data"),
		slog.Duration("duration", 150*time.Millisecond),
	)

	// Example 3: Error with stack trace
	otelLogger.ErrorContext(ctx, "Database failure",
		slog.String("error", "connection timeout"),
		slog.String("db.connection_string", "postgres://user@localhost"),
		slog.Int("retry_count", 3),
	)

	// Example 4: Cloud event (AWS CloudTrail style)
	otelLogger.WarnContext(ctx, "S3 access denied",
		slog.String("cloudtrail.event_source", "s3.amazonaws.com"),
		slog.String("cloudtrail.event_name", "GetObject"),
		slog.String("client.ip", clientIP.String()),
		slog.Int("http.status_code", 403),
	)

	// Example 5: Structured data (Apache-style)
	otelLogger.InfoContext(ctx, "File served",
		slog.String("http.user_agent", "curl/7.68.0"),
		slog.Int("http.response.size", 2456),
		slog.String("network.client.address", clientIP.String()),
	)
}
