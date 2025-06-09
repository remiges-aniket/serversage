package main

// import (
// 	"context"
// 	"errors"
// 	"log"
// 	"log/slog"
// 	"net/http"
// 	"os"
// 	"strings"

// 	"github.com/gin-gonic/gin"
// 	"github.com/remiges-tech/logharbour/logharbour"
// 	"go.opentelemetry.io/contrib/bridges/otelslog"
// 	"go.opentelemetry.io/otel/exporters/otlp/otlplog/otlploghttp"
// 	"go.opentelemetry.io/otel/log/global"
// 	logsdk "go.opentelemetry.io/otel/sdk/log"
// )

// const (
// 	APP_PORT = "8080"
// 	APP_NAME = "my-otel-test-app"
// 	// Define the maximum number of connections in the pool in kafka
// 	POOL_SIZE = 10
// )

// // change this as per kafka state
// var (
// 	IsKafkaOn    = false
// 	KafkaBrokers = []string{"one", "two"}
// 	KafkaTopic   = ""
// )

// type APIResponse struct {
// 	Code    int    `json:"code"`
// 	Message string `json:"message"`
// }
// type DataChangeRequest struct {
// 	Name string `json:"name" form:"name"`
// }

// // OTelWriter implements io.Writer to bridge Logharbour with OpenTelemetry
// type OTelWriter struct {
// 	logger *slog.Logger
// 	ctx    context.Context
// }

// func (w *OTelWriter) Write(p []byte) (n int, err error) {
// 	w.logger.InfoContext(w.ctx, string(p))
// 	return len(p), nil
// }

// func main() {
// 	ctx := context.Background()

// 	// Initialize OpenTelemetry
// 	logExporter, err := otlploghttp.New(ctx)
// 	if err != nil {
// 		log.Fatalf("Failed to create log exporter: %v", err)
// 	}

// 	// start otel provider
// 	loggerProvider := logsdk.NewLoggerProvider(
// 		logsdk.WithProcessor(logsdk.NewBatchProcessor(logExporter)),
// 	)
// 	defer loggerProvider.Shutdown(ctx)
// 	// set it to global
// 	global.SetLoggerProvider(loggerProvider)

// 	// Create OTel logger
// 	otelLogger := otelslog.NewLogger("otel-logger-" + APP_NAME)

// 	// Create a logger context with the default priority.
// 	lctx := logharbour.NewLoggerContext(logharbour.Info)

// 	// creste otel log writer
// 	otelWriter := &OTelWriter{
// 		logger: otelLogger,
// 		ctx:    ctx,
// 	}
// 	//-------------------------------------------------------------------------
// 	var lh *logharbour.Logger
// 	if IsKafkaOn {
// 		// Create a Kafka writer
// 		// Define your Kafka configuration
// 		kafkaConfig := logharbour.KafkaConfig{
// 			Brokers: KafkaBrokers,
// 			Topic:   KafkaTopic,
// 		}

// 		kafkaWriter, err := logharbour.NewKafkaWriter(kafkaConfig, logharbour.WithPoolSize(POOL_SIZE))
// 		if err != nil {
// 			log.Fatalf("Failed to create Kafka writer: %v", err)
// 		}
// 		// Create a fallback writer that uses stdout as the fallback.
// 		fallbackWriter := logharbour.NewFallbackWriter(kafkaWriter, os.Stdout)

// 		// Initialize the logger with the context, validator, default priority, and fallback writer.
// 		lh = logharbour.NewLoggerWithFallback(lctx, APP_NAME, fallbackWriter)

// 		//writer = kafkaWriter
// 	} else {
// 		// lh = logharbour.NewLogger(lctx, consts.APP_NAME, os.Stdout)
// 		lh = logharbour.NewLogger(lctx, APP_NAME, otelWriter).WithInstanceId("111111")
// 		// lctx.ChangeMinLogPriority(currentLogPriority)

// 	}

// 	//--------------------------------------------------------------------------

// 	// pass otel writer to logharbur
// 	lh.Log("Application starting...")

// 	// Setup Gin router
// 	router := gin.Default()

// 	// ---------------------------------------------------------------------  API 1 - Call 1 Api

// 	router.GET("/api/call1", func(c *gin.Context) {
// 		lh.WithModule("xyz")
// 		// Unified logging using Logharbour

// 		lh.LogActivity("Handling call1", map[string]any{
// 			"key": "my log custom",
// 		})
// 		c.JSON(http.StatusOK, APIResponse{
// 			Code:    http.StatusOK,
// 			Message: "API Call 1 Successful",
// 		})
// 	})

// 	// ---------------------------------------------------------------------  API 2 - Datachange Api

// 	router.PUT("/api/datachange", func(c *gin.Context) {
// 		lh.LogActivity("Handling datachange api", map[string]any{
// 			"key": "my log custom",
// 		})

// 		var request DataChangeRequest
// 		if err := c.ShouldBind(&request); err != nil {
// 			lh.Err().Error(err).Log("Invalid request format")
// 			c.JSON(http.StatusBadRequest, APIResponse{
// 				Code:    http.StatusBadRequest,
// 				Message: "Invalid request format",
// 			})
// 			return
// 		}

// 		// Unified logging using Logharbour
// 		old_name := "Vilas"
// 		new_name := request.Name

// 		if !strings.EqualFold(new_name, old_name) {
// 			lh.LogDataChange("name change", logharbour.ChangeInfo{
// 				Entity: "User",
// 				Op:     "Update",
// 				Changes: []logharbour.ChangeDetail{
// 					logharbour.NewChangeDetail("name", old_name, new_name),
// 				},
// 			})
// 		}

// 		c.JSON(http.StatusOK, APIResponse{
// 			Code:    http.StatusOK,
// 			Message: "API Call 2 Successful",
// 		})
// 	})

// 	// ---------------------------------------------------------------------  API 3 - Error Api
// 	router.GET("/api/call3", func(c *gin.Context) {
// 		// Unified logging using Logharbour
// 		lh.LogActivity("Handling call3 error ", map[string]any{
// 			"key": "my log custom",
// 		})
// 		lh.Err().Error(errors.New("my_internal_error")).Log("Server startup failed")

// 		c.JSON(http.StatusInternalServerError, APIResponse{
// 			Code:    http.StatusOK,
// 			Message: "API Call 3 InternalServerError Successful",
// 		})
// 	})

// 	// Start server
// 	if err := router.Run(":" + APP_PORT); err != nil {
// 		lh.Err().Error(err).Log("Server startup failed")
// 	}
// }
