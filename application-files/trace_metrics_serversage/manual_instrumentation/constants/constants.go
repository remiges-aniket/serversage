package constants

const (
	DNS              = "postgres://postgres:postgres@localhost:5432/postgres?sslmode=disable"
	APP_PORT         = ":8080"
	SERVICE_NAME_KEY = "remiges_app"
	OTEL_COLLECTOR   = "go.opentelemetry.io/contrib/examples/otel-collector"
	PGX              = "pgx"
	SERVICE          = "service"

	//API
	ALL_POSTS           = "/posts"
	POST_BY_ID          = "/posts/"
	SIMULATE_EXCEPTION  = "/exception/simulate"
	SIMULATE_DB_ERROR   = "/exception/db-error"
	CUSTOM_EXCEPTION    = "/exception/custom"
	EXCEPTION_LOG_EVENT = "/exception/log-event"

	// otel api constants
	POST_BY_ID_OTEL = "/posts/{id}"
)
