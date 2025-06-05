package main

import (
	"context"
	"log"
	"net/http"

	"example.com/test/new_main/constants"
	"example.com/test/new_main/service"
	"example.com/test/new_main/utils"
	"github.com/prometheus/client_golang/prometheus/promhttp"
	// Using v1.21.0 as specified
)

// emailCounter is the global metric counter for emails.
var (
	//emailCounter metric.Int64Counter
	serviceIP   = utils.GetOutboundIP()
	serviceName = "broadside-email-service"
)

func main() {

	ctx := context.Background()

	log.Printf("Starting email-service with instance ID: %s", serviceIP)

	// Initialize OpenTelemetry metrics with service-level attributes.
	provider, err := service.InitMetrics(serviceName, serviceIP)
	if err != nil {
		log.Fatalf("Failed to initialize metrics: %v", err)
	}

	// Ensure the MeterProvider is shut down when main exits
	defer func() {
		if err := provider.Shutdown(ctx); err != nil {
			log.Printf("Error shutting down MeterProvider: %v", err)
		}
	}()
	//go service.Test()
	service.SimulateMetrics(ctx)

	// Set up the HTTP server to expose Prometheus metrics.
	http.Handle("/metrics", promhttp.Handler())
	log.Println("Prometheus metrics server listening on " + constants.APP_PORT + "/metrics")

	log.Fatal(http.ListenAndServe(constants.APP_PORT, nil))
}
