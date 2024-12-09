package main

import (
	"context"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"

	_ "github.com/jackc/pgx/v5/stdlib" // pgx driver for PostgreSQL
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/trace"
	"go.uber.org/zap"
)

// Post represents a sample data model
type Post struct {
	ID      int    `json:"id"`
	Title   string `json:"title"`
	Content string `json:"content"`
}

var (
	db     *sql.DB
	DNS    string
	logger *zap.Logger
)

func init() {
	// Get PostgreSQL IP address from environment variable
	postgresIP := os.Getenv("POSTGRES_HOST")
	if postgresIP == EMPTY {
		postgresIP = "localhost"
	}
	postgresPort := os.Getenv("POSTGRES_PORT")
	if postgresPort == EMPTY {
		postgresPort = "5432"
	}
	postgresUser := os.Getenv("POSTGRES_USER")
	if postgresUser == EMPTY {
		postgresUser = "postgres"
	}
	postgresPassword := os.Getenv("POSTGRES_PASSWORD")
	if postgresPassword == EMPTY {
		postgresPassword = "postgres"
	}
	postgresDB := os.Getenv("POSTGRES_DB")
	if postgresDB == EMPTY {
		postgresDB = "postgres"
	}

	// Construct the connection string
	DNS = fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		postgresIP, postgresPort, postgresUser, postgresPassword, postgresDB,
	)
}

func main() {
	var (
		err error
	)
	initTracer()

	logger, err = zap.NewDevelopment()
	if err != nil {
		fmt.Printf("error creating zap logger, error:%v", err)
		return
	}

	// Initialize PostgreSQL connection
	db, err = sql.Open(PGX, DNS)
	if err != nil {
		log.Fatalf("Failed to connect to the database: %v", err)
	}
	defer db.Close()

	mux := setupHandler()

	logger.Info("starting http server", zap.String("port", APP_PORT))

	if err := http.ListenAndServe(APP_PORT, mux); err != nil {
		logger.Error("error running server", zap.Error(err))
	}
}

func setupHandler() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc(ALL_POSTS, getAllPosts)
	mux.HandleFunc(POST_BY_ID, postHandler)
	mux.HandleFunc(SIMULATE_EXCEPTION, simulateExceptionHandler)
	mux.HandleFunc(SIMULATE_DB_ERROR, simulateDBErrorHandler)
	mux.HandleFunc(CUSTOM_EXCEPTION, customExceptionHandler)
	mux.HandleFunc(EXCEPTION_LOG_EVENT, logExceptionEventHandler)
	return mux
}

func setupLogharbour() {
	// fallbackWriter := logharbour.NewFallbackWriter(os.Stdout, os.Stdout)

	// lctx := logharbour.NewLoggerContext(logharbour.Info)

	// logger := logharbour.NewLogger(lctx, SERVICE_NAME_KEY, fallbackWriter)

	// logger.LogActivity("User logged in", map[string]any{"username": "aniket"})
	// logger.LogDataChange("User updated profile",
	// 	*logharbour.NewChangeInfo("User", "Update").
	// 		AddChange("email", "aniket@gmail.com", "aniket.shinde@remiges.tech"))

	// logger.LogDebug("Debugging user session", map[string]any{"sessionID": "99693816"})

	// lctx.ChangeMinLogPriority(logharbour.Debug2)

	// logger.LogDebug("Detailed debugging info", map[string]any{"sessionID": "99693816", "userID": "anikets"})
	// logger.Debug0().LogActivity("debug0 test", nil)
}

func initTracer() {

	ctx := context.Background()

	client := otlptracehttp.NewClient()

	otlpTraceExporter, err := otlptrace.New(ctx, client)
	if err != nil {
		log.Fatal(err)
	}

	batchSpanProcessor := trace.NewBatchSpanProcessor(otlpTraceExporter)

	tracerProvider := trace.NewTracerProvider(
		trace.WithSpanProcessor(batchSpanProcessor),
		//trace.WithSampler(sdktrace.AlwaysSample()), - please check TracerProvider.WithSampler() implementation for details.
	)

	otel.SetTracerProvider(tracerProvider)
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{}, propagation.Baggage{}))
}
