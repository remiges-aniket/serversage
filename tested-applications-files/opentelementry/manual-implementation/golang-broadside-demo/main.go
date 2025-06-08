package main

import (
	"context"
	"log"
	"math/rand"
	"net"
	"net/http"
	"time" // Required for time.Now().UnixNano()

	"github.com/prometheus/client_golang/prometheus/promhttp"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/exporters/prometheus"
	"go.opentelemetry.io/otel/metric"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	semconv "go.opentelemetry.io/otel/semconv/v1.21.0" // Using v1.21.0 as specified
)

// emailCounter is the global metric counter for emails.
var (
	emailCounter metric.Int64Counter
	serviceIP    = getOutboundIP()
	serviceName  = "broadside-email-service"
)

// Custom attribute keys for our metrics.
const (
	DCSKey      = attribute.Key("dcs")
	StateKey    = attribute.Key("state")
	ProviderKey = attribute.Key("provider")
	StatusKey   = attribute.Key("status")
	APP_PORT    = ":8088"
)

// initMetrics initializes the OpenTelemetry MeterProvider and Prometheus exporter.
func initMetrics(serviceName, serviceInstanceID string) (*sdkmetric.MeterProvider, error) {
	// 1. Configure the Prometheus exporter.
	exporter, err := prometheus.New()
	if err != nil {
		return nil, err
	}

	// 2. Create the MeterProvider with the exporter and resource.
	provider := sdkmetric.NewMeterProvider(
		sdkmetric.WithReader(exporter),
	)

	// 3. Get a Meter from the provider.
	meter := provider.Meter("bs.email.meter") // Use a unique name for your meter

	// 4. Create the Int64Counter metric.
	emailCounter, err = meter.Int64Counter(
		"bs_email_count",
		metric.WithDescription("Counts emails based on dcs, state, provider, and status"),
	)
	if err != nil {
		return nil, err
	}

	// 5. Register the MeterProvider globally.
	otel.SetMeterProvider(provider)

	return provider, nil
}

// simulateMetrics generates random email events and adds them to the counter.
func simulateMetrics(ctx context.Context) {
	incrementValue := int64(0)
	transit := int64(0)
	sent := int64(0)
	bounced := int64(0)
	rejected := int64(0)

	// Options for random attribute values
	dcsOptions := []string{"dcs1", "lsp", "lsh"}
	providerOptions := []string{"gmail", "hotmail", "outlook", "rediffmail", "others"}
	statusOptions := []string{"in_transit", "sent", "bounced", "rejected"}

	go func() {
		for {
			select {
			case <-ctx.Done():
				return // Exit goroutine if context is cancelled
			default:
				// Choose random label values for each attribute.
				dcs := dcsOptions[rand.Intn(len(dcsOptions))]
				state := rand.Intn(100)
				provider := providerOptions[rand.Intn(len(providerOptions))]
				status := statusOptions[rand.Intn(len(statusOptions))]

				// Ensure a positive and consistent increment value.
				// For a continuously increasing graph, we want a minimum increment.
				// You can adjust this value based on how steep you want the increase.
				incrementValue = 0
				switch status {
				case "in_transit":
					transit = transit + int64(8+rand.Intn(200))
					incrementValue = transit
				case "sent":
					sent = sent + int64(60+rand.Intn(400))
					incrementValue = sent
				case "bounced":
					bounced = bounced + int64(20+rand.Intn(200))
					incrementValue = bounced
				case "rejected":
					rejected = rejected + int64(5+rand.Intn(200))
					incrementValue = rejected
				}
				// Add to the counter with specific attributes for this event.
				emailCounter.Add(ctx, incrementValue,
					// 6. Define the service's resource attributes.
					// These describe the service itself and are attached to all metrics produced by MeterProvider.
					metric.WithAttributes(
						semconv.ServiceNameKey.String(serviceName),
						semconv.ServiceInstanceIDKey.String(serviceIP),
						DCSKey.String(dcs),
						StateKey.Int(state),
						ProviderKey.String(provider),
						StatusKey.String(status),
					),
				)

				// Simulate work/delay before the next event.
				time.Sleep(3 * time.Second)
			}
		}
	}()
}

// getOutboundIP attempts to determine the system's preferred outbound IP address.
func getOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80") // Connect to a public DNS server (no data sent)
	if err != nil {
		log.Printf("Warning: Could not determine outbound IP. Using 'unknown_ip'. Error: %v", err)
		return "unknown_ip"
	}
	defer conn.Close() // Ensure the connection is closed

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}

func main() {

	ctx := context.Background()

	log.Printf("Starting email-service with instance ID: %s", serviceIP)

	// Initialize OpenTelemetry metrics with service-level attributes.
	provider, err := initMetrics(serviceName, serviceIP)
	if err != nil {
		log.Fatalf("Failed to initialize metrics: %v", err)
	}

	// Ensure the MeterProvider is shut down when main exits
	defer func() {
		if err := provider.Shutdown(ctx); err != nil {
			log.Printf("Error shutting down MeterProvider: %v", err)
		}
	}()

	simulateMetrics(ctx)

	// Set up the HTTP server to expose Prometheus metrics.
	http.Handle("/metrics", promhttp.Handler())
	log.Println("Prometheus metrics server listening on " + APP_PORT + "/metrics")

	log.Fatal(http.ListenAndServe(APP_PORT, nil))
}
