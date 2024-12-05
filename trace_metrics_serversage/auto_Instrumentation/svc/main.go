package main

import (
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"

	_ "github.com/jackc/pgx/v5/stdlib" // pgx driver for PostgreSQL
)

// Post represents a sample data model
type Post struct {
	ID      int    `json:"id"`
	Title   string `json:"title"`
	Content string `json:"content"`
}

var (
	db *sql.DB
)

func main() {
	var (
		err error
		// Default_DNS = "postgres://postgres:postgres@localhost:5432/postgres?sslmode=disable"
	)

	// Get PostgreSQL IP address from environment variable
	postgresIP := os.Getenv("POSTGRES_IP")
	if postgresIP == EMPTY {
		postgresIP = "localhost"
	}
	postgresPort := os.Getenv("POSTGRES_IP")
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
	DNS := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		postgresIP, postgresPort, postgresUser, postgresPassword, postgresDB,
	)

	//----------------- Logharbour -----------------//
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
	//----------------- Logharbour End -----------------//

	// Initialize PostgreSQL connection
	db, err = sql.Open(PGX, DNS)
	if err != nil {
		log.Fatalf("Failed to connect to the database: %v", err)
	}
	defer db.Close()

	// HTTP routes
	http.HandleFunc(ALL_POSTS, getAllPosts)
	http.HandleFunc(POST_BY_ID, postHandler)
	http.HandleFunc(SIMULATE_EXCEPTION, simulateExceptionHandler)
	http.HandleFunc(SIMULATE_DB_ERROR, simulateDBErrorHandler)
	http.HandleFunc(CUSTOM_EXCEPTION, customExceptionHandler)
	http.HandleFunc(EXCEPTION_LOG_EVENT, logExceptionEventHandler)

	// Start the HTTP server
	log.Printf("Server started on %s \n", APP_PORT)
	log.Fatal(http.ListenAndServe(APP_PORT, nil))
}
