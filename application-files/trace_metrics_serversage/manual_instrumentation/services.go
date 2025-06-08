package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"
	"test.co/demo/constants"
)

// getAllPosts fetches all posts from the database
func getAllPosts(w http.ResponseWriter, r *http.Request) {
	log.Println("getAllPosts")
	_, span := otel.Tracer("getAllPosts").Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, "/posts"))
	defer span.End()

	span.SetAttributes(attribute.String("http.method", r.Method), attribute.String("http.path", "/posts"), attribute.Int("http.status_code", http.StatusOK))

	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	span.AddEvent("SELECT id, title, content FROM posts")
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
func postHandler(w http.ResponseWriter, r *http.Request) {
	ctx, span := otel.Tracer("postHandler").Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, constants.POST_BY_ID_OTEL))
	defer span.End()
	idStr := r.URL.Path[len(constants.POST_BY_ID):]
	id, err := strconv.Atoi(idStr)
	if err != nil && idStr != "" {
		http.Error(w, "Invalid ID", http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodGet:
		getPostByID(w, id, &ctx)
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
	var post Post
	_, span := otel.Tracer("getPostByIDProcessor").Start(*ctx, fmt.Sprintf("%s %s %d", constants.SERVICE, constants.POST_BY_ID, id))
	span.SetAttributes(attribute.String("query", "getPostByIDProcessor"), attribute.Int("http.status_code", http.StatusOK))
	defer span.End()

	if id == 0 {
		http.Error(w, "ID is required", http.StatusBadRequest)
		return
	}

	span.AddEvent(fmt.Sprintf("SELECT id, title, content FROM posts WHERE id = %d", id))
	err := db.QueryRow("SELECT id, title, content FROM posts WHERE id = $1", id).Scan(&post.ID, &post.Title, &post.Content)
	if err != nil {
		http.Error(w, fmt.Sprintf("QueryRow error: %v", err), http.StatusNotFound)
		return
	}

	span.AddEvent(fmt.Sprintf("successfully fetched ID: %d, from database", id))

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

// simulateExceptionHandler creates a sample exception for tracing.
func simulateExceptionHandler(w http.ResponseWriter, r *http.Request) {
	tracer := otel.Tracer("simulateExceptionHandler")
	_, span := tracer.Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, "/exception/simulate"))
	defer span.End()
	span.SetAttributes(attribute.String("http.method", r.Method), attribute.String("http.path", "/exception/simulate"), attribute.Int("http.status_code", http.StatusExpectationFailed), attribute.String("exception", "true"))
	err := fmt.Errorf("simulated exception occurred")
	span.RecordError(err)

	http.Error(w, "Simulated exception", http.StatusExpectationFailed)
}

// simulateDBErrorHandler simulates a database error.
func simulateDBErrorHandler(w http.ResponseWriter, r *http.Request) {
	tracer := otel.Tracer("simulateDBErrorHandler")
	_, span := tracer.Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, "/exception/db-error"))
	defer span.End()

	span.SetAttributes(attribute.String("query", "SELECT * FROM non_existing_table"), attribute.Int("http.status_code", http.StatusInternalServerError))
	err := fmt.Errorf("database query failed due to missing table")
	span.RecordError(err)

	http.Error(w, "Simulated database error", http.StatusInternalServerError)
}

// customExceptionHandler logs a custom error with metadata.
func customExceptionHandler(w http.ResponseWriter, r *http.Request) {
	tracer := otel.Tracer("customExceptionHandler")
	_, span := tracer.Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, "/exception/custom"))
	defer span.End()

	metadata := attribute.String("custom.meta", "Demo metadata for custom exception")
	span.SetAttributes(metadata, attribute.Int("http.status_code", http.StatusBadRequest))

	err := fmt.Errorf("custom exception example")
	span.RecordError(err)

	http.Error(w, "Custom exception logged", http.StatusBadRequest)
}

// logExceptionEventHandler logs an event to Jaeger during an operation.
func logExceptionEventHandler(w http.ResponseWriter, r *http.Request) {
	tracer := otel.Tracer("logExceptionEventHandler")
	_, span := tracer.Start(r.Context(), fmt.Sprintf("%s  %s", r.Method, "/exception/log-event"))
	defer span.End()

	span.AddEvent("Critical issue occurred during process", trace.WithAttributes(
		attribute.String("step", "critical-section"), attribute.Int("http.status_code", http.StatusBadGateway),
	))

	http.Error(w, "Exception event logged", http.StatusBadGateway)
}
