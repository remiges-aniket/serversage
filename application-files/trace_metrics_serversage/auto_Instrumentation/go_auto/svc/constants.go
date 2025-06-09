package main

const (
	APP_PORT         = ":8080"
	SERVICE_NAME_KEY = "remiges_app"
	OTEL_COLLECTOR   = "go.opentelemetry.io/contrib/examples/otel-collector"
	PGX              = "pgx"
	SERVICE          = "service"
	CONTENT_TYPE     = "Content-Type"
	JSON             = "application/json"
	EMPTY            = ""

	//API
	ALL_POSTS           = "/posts"
	POST_BY_ID          = "/posts/"
	SIMULATE_EXCEPTION  = "/exception/simulate"
	SIMULATE_DB_ERROR   = "/exception/db-error"
	CUSTOM_EXCEPTION    = "/exception/custom"
	EXCEPTION_LOG_EVENT = "/exception/log-event"

	// otel api constants
	POST_BY_ID_OTEL = "/posts/{id}"

	// db query constants
	GET_ALL_POSTS_QRY  = "SELECT id, title, content FROM posts"
	GET_POST_BY_ID_QRY = "SELECT id, title, content FROM posts WHERE id = $1"
	INSERT_POST_QRY    = "INSERT INTO posts (title, content) VALUES ($1, $2) RETURNING id"
	UPDATE_POST_QRY    = "UPDATE posts SET title = $1, content = $2 WHERE id = $3"
	DELETE_POST_QRY    = "DELETE FROM posts WHERE id = $1"

	// error constants
	INVALID_JSON = "Invalid JSON"
	ID_REQUIRED  = "ID is required"
	INVALID_ID   = "Invalid ID"
	NOT_ALLOWED  = "Method not allowed"

	//error messages
	SIMULATED_EXCEPTION_MSG = "simulated exception occurred"
	DB_EXCEPTION_MSG        = "database query failed due to missing table"
	CUSTOM_EXCEPTION_MSG    = "custom exception example"
	LOG_EXCEPTION_MSG       = "exception event logged"
)
