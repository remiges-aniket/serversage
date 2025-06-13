package service

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"math/rand"
	"net/http"
	"time"

	"example.com/test/new_main/constants"
	model "example.com/test/new_main/model/response"
	"example.com/test/new_main/utils"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/prometheus"
	"go.opentelemetry.io/otel/metric"
	sdkmetric "go.opentelemetry.io/otel/sdk/metric"
	semconv "go.opentelemetry.io/otel/semconv/v1.4.0"
)

var (
	emailCounter metric.Int64Counter
	serviceIP    = utils.GetOutboundIP()
	serviceName  = "broadside-email-service"
)

func Test(ctx context.Context) {

	now := time.Now().UTC()
	start_time := now.Add(-1 * time.Hour)
	end_time := now
	start_time_str := start_time.Format(time.RFC3339)
	end_time_str := end_time.Format(time.RFC3339)
	query := fmt.Sprintf("bml_dispatchedat:[%s TO %s]", start_time_str, end_time_str)

	resp, err := http.Get("https://reqres.in/api/users?page=2")

	if err != nil {
		panic(err)
	}
	body, _ := io.ReadAll(resp.Body)

	var request model.SolrResponse
	if err := json.Unmarshal(body, &request); err != nil {
		panic(err)
	}

	request.ResponseHeader.Params.Query = query
	log.Println(resp.Body)
	//dcs:=

	dcsOptions := []string{"dcs1", "lsp", "lsh"}
	providerOptions := []string{"gmail", "hotmail", "outlook", "rediffmail", "others"}

	//time.Sleep(15 * time.Second)
	// Choose random label values for each attribute.
	dcs := dcsOptions[rand.Intn(len(dcsOptions))]
	state := rand.Intn(100)
	provider := providerOptions[rand.Intn(len(providerOptions))]

	//bml_status_map := utils.ConvertBMLStatus(request.FacetCounts.FacetFields.BMLStatus)

	statusCountMap := map[string]int{
		"in_transit": 13,
		"sent":       12,
		"bounced":    10,
		"rejected":   033,
	}
	bml_status_map := statusCountMap
	// var transit_count, sent_count, bounced_count, rejected_count, total int64
	// var transit_status, sent_status, bounced_status, rejected_status, total_status string
	total := int64(request.Response.NumFound)
	total_status := "total"
	bml_status_map[total_status] = int(total)
	fmt.Println(bml_status_map)

	for status, count := range bml_status_map {

		emailCounter.Add(ctx, int64(count),
			// 6. Define the service's resource attributes.
			// These describe the service itself and are attached to all metrics produced by MeterProvider.
			metric.WithAttributes(
				semconv.ServiceNameKey.String(serviceName),
				semconv.ServiceInstanceIDKey.String(serviceIP),
				constants.DCSKey.String(dcs),
				constants.StateKey.Int(state),
				constants.ProviderKey.String(provider),
				constants.StatusKey.String(status),
			),
		)
	}

}

// SimulateMetrics generates random email events and adds them to the counter.
func SimulateMetrics(ctx context.Context) {

	go func() {
		for {
			select {
			case <-ctx.Done():
				return // Exit goroutine if context is cancelled
			default:

				Test(ctx)

				// Simulate work/delay before the next event.
				time.Sleep(15 * time.Second)
			}
		}
	}()
}

// initMetrics initializes the OpenTelemetry MeterProvider and Prometheus exporter.
func InitMetrics(serviceName, serviceInstanceID string) (*sdkmetric.MeterProvider, error) {
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
