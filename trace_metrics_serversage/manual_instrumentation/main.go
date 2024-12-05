package main

import (
	"context"
	"database/sql"
	"log"
	"net/http"
	"os"
	"os/signal"
	"sync"

	_ "github.com/jackc/pgx/v5/stdlib" // pgx driver for PostgreSQL
	"github.com/remiges-tech/logharbour/logharbour"
	"test.co/demo/constants"

	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
	"go.opentelemetry.io/otel/sdk/resource"
)

// Post represents a sample data model
type Post struct {
	ID      int    `json:"id"`
	Title   string `json:"title"`
	Content string `json:"content"`
}

var (
	db                *sql.DB
	initResourcesOnce sync.Once
	rsorce            *resource.Resource
)

func main() {
	//----------------- Logharbour -----------------//
	fallbackWriter := logharbour.NewFallbackWriter(os.Stdout, os.Stdout)

	lctx := logharbour.NewLoggerContext(logharbour.Info)

	logger := logharbour.NewLogger(lctx, constants.SERVICE_NAME_KEY, fallbackWriter)

	logger.LogActivity("User logged in", map[string]any{"username": "aniket"})
	logger.LogDataChange("User updated profile",
		*logharbour.NewChangeInfo("User", "Update").
			AddChange("email", "aniket@gmail.com", "aniket.shinde@remiges.tech"))

	logger.LogDebug("Debugging user session", map[string]any{"sessionID": "99693816"})

	lctx.ChangeMinLogPriority(logharbour.Debug2)

	logger.LogDebug("Detailed debugging info", map[string]any{"sessionID": "99693816", "userID": "anikets"})
	logger.Debug0().LogActivity("debug0 test", nil)
	//----------------- Logharbour End -----------------//

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt)
	defer cancel()

	shutdownTracerProvider, err := initTracerProvider(ctx)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := shutdownTracerProvider(ctx); err != nil {
			log.Fatalf("failed to shutdown TracerProvider: %s", err)
		}
	}()

	shutdownMeterProvider, err := initMeterProvider(ctx)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := shutdownMeterProvider(ctx); err != nil {
			log.Fatalf("failed to shutdown MeterProvider: %s", err)
		}
	}()

	// Initialize PostgreSQL connection
	db, err = sql.Open(constants.PGX, constants.DNS)
	if err != nil {
		log.Fatalf("Failed to connect to the database: %v", err)
	}
	defer db.Close()

	// HTTP routes
	http.Handle(constants.ALL_POSTS, otelhttp.NewHandler(http.HandlerFunc(getAllPosts), constants.ALL_POSTS))
	http.Handle(constants.POST_BY_ID, otelhttp.NewHandler(http.HandlerFunc(postHandler), constants.POST_BY_ID_OTEL))
	http.Handle(constants.SIMULATE_EXCEPTION, otelhttp.NewHandler(http.HandlerFunc(simulateExceptionHandler), constants.SIMULATE_EXCEPTION))
	http.Handle(constants.SIMULATE_DB_ERROR, otelhttp.NewHandler(http.HandlerFunc(simulateDBErrorHandler), constants.SIMULATE_DB_ERROR))
	http.Handle(constants.CUSTOM_EXCEPTION, otelhttp.NewHandler(http.HandlerFunc(customExceptionHandler), constants.CUSTOM_EXCEPTION))
	http.Handle(constants.EXCEPTION_LOG_EVENT, otelhttp.NewHandler(http.HandlerFunc(logExceptionEventHandler), constants.EXCEPTION_LOG_EVENT))

	// Start the HTTP server
	log.Printf("Server started on %s \n", constants.APP_PORT)
	log.Fatal(http.ListenAndServe(constants.APP_PORT, nil))
}
