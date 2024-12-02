package main

import (
	"context"
	"database/sql"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"strconv"
	"sync"
	"time"

	_ "github.com/jackc/pgx/v5/stdlib" // pgx driver for PostgreSQL
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/metric"
	"go.opentelemetry.io/otel/sdk/resource"
	semconv "go.opentelemetry.io/otel/semconv/v1.4.0"
	"go.opentelemetry.io/otel/trace"
)

// Post represents a sample data model
type Post struct {
	ID      int    `json:"id"`
	Title   string `json:"title"`
	Content string `json:"content"`
}

const ( // Replace with your credentials
	dsn                = "postgres://postgres:postgres@localhost:5432/postgres?sslmode=disable"
	app_port           = ":8080"
	otel_grpc_endpoint = "localhost:4317"
	service_name_key   = "remiges.service"
	otel_collector     = "go.opentelemetry.io/contrib/examples/otel-collector"
)

var (
	service_name      = semconv.ServiceNameKey.String(service_name_key)
	db                *sql.DB
	initResourcesOnce sync.Once
	rsorce            *resource.Resource
)

type payload struct {
	ctx *context.Context
}

func main() {
	var err error

	ctx, cancel := signal.NotifyContext(context.Background(), os.Interrupt)
	defer cancel()

	conn, err := initConn()
	if err != nil {
		log.Fatal(err)
	}

	shutdownTracerProvider, err := initTracerProvider(ctx, conn)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := shutdownTracerProvider(ctx); err != nil {
			log.Fatalf("failed to shutdown TracerProvider: %s", err)
		}
	}()

	shutdownMeterProvider, err := initMeterProvider(ctx, conn)
	if err != nil {
		log.Fatal(err)
	}
	defer func() {
		if err := shutdownMeterProvider(ctx); err != nil {
			log.Fatalf("failed to shutdown MeterProvider: %s", err)
		}
	}()

	tracer := otel.Tracer(otel_collector)
	meter := otel.Meter(otel_collector)

	// Attributes represent additional key-value descriptors that can be bound
	// to a metric observer or recorder.
	commonAttrs := []attribute.KeyValue{
		attribute.String("attrA", "chocolate"),
		attribute.String("attrB", "raspberry"),
		attribute.String("attrC", "vanilla"),
	}

	runCount, err := meter.Int64Counter("run", metric.WithDescription("The number of times the iteration ran"))
	if err != nil {
		log.Fatal(err)
	}
	// Work begins
	ctx, span := tracer.Start(
		ctx,
		service_name_key,
		trace.WithAttributes(commonAttrs...))
	defer span.End()

	// Initialize PostgreSQL connection
	db, err = sql.Open("pgx", dsn)
	if err != nil {
		log.Fatalf("Failed to connect to the database: %v", err)
	}
	defer db.Close()

	// Ensure the database is reachable
	if err := db.Ping(); err != nil {
		log.Fatalf("Database ping failed: %v", err)
	}

	for i := 0; i < 8; i++ {
		_, iSpan := tracer.Start(ctx, fmt.Sprintf("Sample-%d", i))
		runCount.Add(ctx, 1, metric.WithAttributes(attribute.String("org", "remiges tech")))
		log.Printf("Sending test trace (%d / %d)\n", i+1, 8)

		<-time.After(time.Second)
		iSpan.End()
	}

	payload := &payload{
		ctx: &ctx,
	}
	// HTTP routes
	http.HandleFunc("/posts", payload.getAllPosts)
	http.HandleFunc("/posts/", payload.postHandler)

	log.Println("calling adding metrics")
	log.Println("exit adding metrics")

	// Start the HTTP server
	log.Printf("Server started on %s \n", app_port)
	log.Fatal(http.ListenAndServe(app_port, nil))
}

// getAllPosts fetches all posts from the database
func (pylod *payload) getAllPosts(w http.ResponseWriter, r *http.Request) {
	log.Println("getAllPosts")

	tracer := otel.Tracer("getAllPosts")
	_, span := tracer.Start(*pylod.ctx, "getAllPosts")
	defer span.End()
	span.SetAttributes(attribute.String("http.method", r.Method), attribute.String("http.path", "/posts"))

	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	rows, err := db.Query("SELECT id, title, content FROM posts")
	if err != nil {
		span.RecordError(err)
		http.Error(w, fmt.Sprintf("Query error: %v", err), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var posts []Post
	for rows.Next() {
		var post Post
		if err := rows.Scan(&post.ID, &post.Title, &post.Content); err != nil {
			span.RecordError(err)
			http.Error(w, fmt.Sprintf("Row scan error: %v", err), http.StatusInternalServerError)
			return
		}
		posts = append(posts, post)
	}

	// add log msg to span
	span.AddEvent("Fetched posts from database")
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(posts)
}

// postHandler handles specific post-related requests (Create, Read by ID, Update, Delete)
func (pylod *payload) postHandler(w http.ResponseWriter, r *http.Request) {

	idStr := r.URL.Path[len("/posts/"):]
	id, err := strconv.Atoi(idStr)
	if err != nil && idStr != "" {
		http.Error(w, "Invalid ID", http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodGet:
		getPostByID(w, id, pylod.ctx)
	case http.MethodPost:
		createPost(w, r)
	case http.MethodPut:
		updatePost(w, r, id)
	case http.MethodDelete:
		deletePost(w, id)
	default:
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
	}
}

// getPostByID fetches a post by its ID
func getPostByID(w http.ResponseWriter, id int, ctx *context.Context) {
	tracer := otel.Tracer("getPostByIDHandler")
	_, iSpan := tracer.Start(*ctx, fmt.Sprintf("ID : %d", id))
	iSpan.SetAttributes(attribute.String("query", "getPostByIDHandler"))
	iSpan.AddEvent("Querying database")

	if id == 0 {
		http.Error(w, "ID is required", http.StatusBadRequest)
		return
	}

	var post Post
	err := db.QueryRow("SELECT id, title, content FROM posts WHERE id = $1", id).Scan(&post.ID, &post.Title, &post.Content)
	if err != nil {
		http.Error(w, fmt.Sprintf("QueryRow error: %v", err), http.StatusNotFound)
		return
	}

	defer iSpan.End()
	iSpan.AddEvent(fmt.Sprintf("successfully fetched ID: %d, from database", id))

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(post)
}

// createPost creates a new post in the database
func createPost(w http.ResponseWriter, r *http.Request) {
	var post Post
	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	err := db.QueryRow("INSERT INTO posts (title, content) VALUES ($1, $2) RETURNING id", post.Title, post.Content).Scan(&post.ID)
	if err != nil {
		http.Error(w, fmt.Sprintf("Insert error: %v", err), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(post)
}

// updatePost updates a post by its ID
func updatePost(w http.ResponseWriter, r *http.Request, id int) {
	if id == 0 {
		http.Error(w, "ID is required", http.StatusBadRequest)
		return
	}

	var post Post
	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
		http.Error(w, "Invalid JSON", http.StatusBadRequest)
		return
	}

	_, err := db.Exec("UPDATE posts SET title = $1, content = $2 WHERE id = $3", post.Title, post.Content, id)
	if err != nil {
		http.Error(w, fmt.Sprintf("Update error: %v", err), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// deletePost deletes a post by its ID
func deletePost(w http.ResponseWriter, id int) {
	if id == 0 {
		http.Error(w, "ID is required", http.StatusBadRequest)
		return
	}

	_, err := db.Exec("DELETE FROM posts WHERE id = $1", id)
	if err != nil {
		http.Error(w, fmt.Sprintf("Delete error: %v", err), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
