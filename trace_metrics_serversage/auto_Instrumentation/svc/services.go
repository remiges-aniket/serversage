package main

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"strconv"
)

// getAllPosts fetches all posts from the database
func getAllPosts(w http.ResponseWriter, r *http.Request) {
	log.Println("getAllPosts")

	if r.Method != http.MethodGet {
		http.Error(w, NOT_ALLOWED, http.StatusMethodNotAllowed)
		return
	}

	rows, err := db.Query(GET_ALL_POSTS_QRY)
	if err != nil {
		http.Error(w, fmt.Sprintf("Query error: %v", err), http.StatusInternalServerError)
		return
	}
	defer rows.Close()

	var posts []Post
	for rows.Next() {
		var post Post
		if err := rows.Scan(&post.ID, &post.Title, &post.Content); err != nil {
			http.Error(w, fmt.Sprintf("Row scan error: %v", err), http.StatusInternalServerError)
			return
		}
		posts = append(posts, post)
	}

	w.Header().Set(CONTENT_TYPE, JSON)
	json.NewEncoder(w).Encode(posts)
}

// postHandler handles specific post-related requests (Create, Read by ID, Update, Delete)
func postHandler(w http.ResponseWriter, r *http.Request) {
	idStr := r.URL.Path[len(POST_BY_ID):]
	id, err := strconv.Atoi(idStr)
	if err != nil && idStr != EMPTY {
		http.Error(w, INVALID_ID, http.StatusBadRequest)
		return
	}

	switch r.Method {
	case http.MethodGet:
		getPostByID(w, id)
	case http.MethodPost:
		createPost(w, r)
	case http.MethodPut:
		updatePost(w, r, id)
	case http.MethodDelete:
		deletePost(w, id)
	default:
		http.Error(w, NOT_ALLOWED, http.StatusMethodNotAllowed)
	}
}

// getPostByID fetches a post by its ID
func getPostByID(w http.ResponseWriter, id int) {
	var post Post
	if id == 0 {
		http.Error(w, ID_REQUIRED, http.StatusBadRequest)
		return
	}

	err := db.QueryRow(GET_POST_BY_ID_QRY, id).Scan(&post.ID, &post.Title, &post.Content)
	if err != nil {
		http.Error(w, fmt.Sprintf("QueryRow error: %v", err), http.StatusNotFound)
		return
	}

	w.Header().Set(CONTENT_TYPE, JSON)
	json.NewEncoder(w).Encode(post)
}

// createPost creates a new post in the database
func createPost(w http.ResponseWriter, r *http.Request) {
	var post Post
	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
		http.Error(w, INVALID_JSON, http.StatusBadRequest)
		return
	}

	err := db.QueryRow(INSERT_POST_QRY, post.Title, post.Content).Scan(&post.ID)
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
		http.Error(w, ID_REQUIRED, http.StatusBadRequest)
		return
	}

	var post Post
	if err := json.NewDecoder(r.Body).Decode(&post); err != nil {
		http.Error(w, INVALID_JSON, http.StatusBadRequest)
		return
	}

	_, err := db.Exec(UPDATE_POST_QRY, post.Title, post.Content, id)
	if err != nil {
		http.Error(w, fmt.Sprintf("Update error: %v", err), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// deletePost deletes a post by its ID
func deletePost(w http.ResponseWriter, id int) {
	if id == 0 {
		http.Error(w, ID_REQUIRED, http.StatusBadRequest)
		return
	}

	_, err := db.Exec(DELETE_POST_QRY, id)
	if err != nil {
		http.Error(w, fmt.Sprintf("Delete error: %v", err), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}

// simulateExceptionHandler creates a sample exception for tracing.
func simulateExceptionHandler(w http.ResponseWriter, r *http.Request) {

	http.Error(w, SIMULATED_EXCEPTION_MSG, http.StatusExpectationFailed)
}

// simulateDBErrorHandler simulates a database error.
func simulateDBErrorHandler(w http.ResponseWriter, r *http.Request) {

	http.Error(w, DB_EXCEPTION_MSG, http.StatusInternalServerError)
}

// customExceptionHandler logs a custom error with metadata.
func customExceptionHandler(w http.ResponseWriter, r *http.Request) {

	http.Error(w, CUSTOM_EXCEPTION_MSG, http.StatusBadRequest)
}

// logExceptionEventHandler logs an event to Jaeger during an operation.
func logExceptionEventHandler(w http.ResponseWriter, r *http.Request) {

	http.Error(w, LOG_EXCEPTION_MSG, http.StatusBadGateway)
}
