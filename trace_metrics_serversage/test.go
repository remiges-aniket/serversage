package main

// import (
// 	"context"
// 	"database/sql"
// 	"encoding/json"
// 	"fmt"
// 	"log"
// 	"net/http"
// 	"os"
// 	"os/signal"
// 	"strconv"
// 	"syscall"
// 	"time"

// 	_ "github.com/jackc/pgx/v5/stdlib" // pgx driver for PostgreSQL
// 	otelhooks "github.com/open-feature/go-sdk-contrib/hooks/open-telemetry/pkg"
// 	flagd "github.com/open-feature/go-sdk-contrib/providers/flagd/pkg"
// 	"github.com/open-feature/go-sdk/openfeature"
// 	"go.opentelemetry.io/contrib/instrumentation/net/http/otelhttp"
// 	"go.opentelemetry.io/contrib/instrumentation/runtime"
// 	"go.opentelemetry.io/otel/attribute"
// 	"go.opentelemetry.io/otel/trace"
// )

// // Post represents a sample data model
// type Post struct {
// 	ID      int    `json:"id"`
// 	Title   string `json:"title"`
// 	Content string `json:"content"`
// }

// var (
// 	db *sql.DB
// )

// const ( // Replace with your credentials
// 	dsn      = "postgres://postgres:postgres@localhost:5432/postgres?sslmode=disable"
// 	APP_PORT = ":8080"
// )

// func main() {
// 	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
// 	defer stop()

// 	// Initialize telemetry
// 	tp := initTracerProvider()
// 	defer func() {
// 		if err := tp.Shutdown(ctx); err != nil {
// 			log.Fatalf("Error shutting down tracer provider: %v", err)
// 		}
// 	}()
// 	mp := initMeterProvider()
// 	defer func() {
// 		if err := mp.Shutdown(ctx); err != nil {
// 			log.Fatalf("Error shutting down meter provider: %v", err)
// 		}
// 	}()
// 	openfeature.AddHooks(otelhooks.NewTracesHook())
// 	err := openfeature.SetProvider(flagd.NewProvider())
// 	if err != nil {
// 		log.Fatalf("Failed to set OpenFeature provider: %v", err)
// 	}

// 	err = runtime.Start(runtime.WithMinimumReadMemStatsInterval(time.Second))
// 	if err != nil {
// 		log.Fatalf("Failed to start runtime instrumentation: %v", err)
// 	}

// 	// Initialize DB
// 	db, err = sql.Open("pgx", dsn)
// 	if err != nil {
// 		log.Fatalf("Failed to connect to database: %v", err)
// 	}
// 	defer db.Close()
// 	if err := db.Ping(); err != nil {
// 		log.Fatalf("Database connection error: %v", err)
// 	}

// 	// Start HTTP server
// 	startHTTPServer(ctx)
// }

// // startHTTPServer starts the HTTP server with OpenTelemetry middleware
// func startHTTPServer(ctx context.Context) {
// 	mux := http.NewServeMux()
// 	mux.HandleFunc("/posts", getAllPosts)
// 	mux.HandleFunc("/posts/", postHandler)

// 	server := &http.Server{
// 		Addr:    APP_PORT,
// 		Handler: otelhttp.NewHandler(mux, "HTTPServer"),
// 	}

// 	go func() {
// 		log.Printf("HTTP server running on %s\n", APP_PORT)
// 		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
// 			log.Fatalf("HTTP server error: %v", err)
// 		}
// 	}()

// 	<-ctx.Done()
// 	shutdownCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
// 	defer cancel()
// 	if err := server.Shutdown(shutdownCtx); err != nil {
// 		log.Fatalf("HTTP server shutdown error: %v", err)
// 	}
// }

// // getAllPosts fetches all posts from the database
// func getAllPosts(w http.ResponseWriter, r *http.Request) {
// 	log.Println("getAllPosts")
// 	ctx := r.Context()
// 	span := trace.SpanFromContext(ctx)
// 	defer span.End()

// 	span.SetAttributes(attribute.String("http.method", r.Method), attribute.String("http.path", "/posts"))

// 	if r.Method != http.MethodGet {
// 		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
// 		return
// 	}

// 	rows, err := db.Query("SELECT id, title, content FROM posts")
// 	if err != nil {
// 		span.RecordError(err)
// 		http.Error(w, fmt.Sprintf("Query error: %v", err), http.StatusInternalServerError)
// 		return
// 	}
// 	defer rows.Close()

// 	var posts []Post
// 	for rows.Next() {
// 		var post Post
// 		if err := rows.Scan(&post.ID, &post.Title, &post.Content); err != nil {
// 			span.RecordError(err)
// 			http.Error(w, fmt.Sprintf("Row scan error: %v", err), http.StatusInternalServerError)
// 			return
// 		}
// 		posts = append(posts, post)
// 	}

// 	span.AddEvent("Fetched posts from database")
// 	w.Header().Set("Content-Type", "application/json")
// 	json.NewEncoder(w).Encode(posts)
// }

// // postHandler handles specific post-related requests (Create, Read by ID, Update, Delete)
// func postHandler(w http.ResponseWriter, r *http.Request) {
// 	ctx := r.Context()
// 	span := trace.SpanFromContext(ctx)
// 	defer span.End()

// 	idStr := r.URL.Path[len("/posts/"):]
// 	id, err := strconv.Atoi(idStr)
// 	if err != nil && idStr != "" {
// 		span.RecordError(err)
// 		http.Error(w, "Invalid ID", http.StatusBadRequest)
// 		return
// 	}

// 	span.SetAttributes(attribute.Int("post.id", id))

// 	switch r.Method {
// 	case http.MethodGet:
// 		getPostByID(w, id)
// 	case http.MethodPost:
// 		createPost(w, r)
// 	case http.MethodPut:
// 		updatePost(w, r, id)
// 	case http.MethodDelete:
// 		deletePost(w, id)
// 	default:
// 		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
// 	}
// }

// // getPostByID fetches a post by its ID
// func getPostByID(w http.ResponseWriter, id int) {

// 	if id == 0 {
// 		http.Error(w, "ID is required", http.StatusBadRequest)
// 		return
// 	}

// 	var post Post
// 	err := db.QueryRow("SELECT id, title, content FROM posts WHERE id = $1", id).Scan(&post.ID, &post.Title, &post.Content)
// 	if err != nil {
// 		http.Error(w, fmt.Sprintf("QueryRow error: %v", err), http.StatusNotFound)
// 		return
// 	}

// 	w.Header().Set("Content-Type", "application/json")
// 	json.NewEncoder(w).Encode(post)
// }

// // createPost creates a new post in the database
// func createPost(w http.ResponseWriter, r *http.Request) {
// 	var post Post
// 	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
// 		http.Error(w, "Invalid JSON", http.StatusBadRequest)
// 		return
// 	}

// 	err := db.QueryRow("INSERT INTO posts (title, content) VALUES ($1, $2) RETURNING id", post.Title, post.Content).Scan(&post.ID)
// 	if err != nil {
// 		http.Error(w, fmt.Sprintf("Insert error: %v", err), http.StatusInternalServerError)
// 		return
// 	}

// 	w.WriteHeader(http.StatusCreated)
// 	json.NewEncoder(w).Encode(post)
// }

// // updatePost updates a post by its ID
// func updatePost(w http.ResponseWriter, r *http.Request, id int) {
// 	if id == 0 {
// 		http.Error(w, "ID is required", http.StatusBadRequest)
// 		return
// 	}

// 	var post Post
// 	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
// 		http.Error(w, "Invalid JSON", http.StatusBadRequest)
// 		return
// 	}

// 	_, err := db.Exec("UPDATE posts SET title = $1, content = $2 WHERE id = $3", post.Title, post.Content, id)
// 	if err != nil {
// 		http.Error(w, fmt.Sprintf("Update error: %v", err), http.StatusInternalServerError)
// 		return
// 	}

// 	w.WriteHeader(http.StatusNoContent)
// }

// // deletePost deletes a post by its ID
// func deletePost(w http.ResponseWriter, id int) {
// 	if id == 0 {
// 		http.Error(w, "ID is required", http.StatusBadRequest)
// 		return
// 	}

// 	_, err := db.Exec("DELETE FROM posts WHERE id = $1", id)
// 	if err != nil {
// 		http.Error(w, fmt.Sprintf("Delete error: %v", err), http.StatusInternalServerError)
// 		return
// 	}

// 	w.WriteHeader(http.StatusNoContent)
// }
