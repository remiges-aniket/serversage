package constants

import "go.opentelemetry.io/otel/attribute"

// Custom attribute keys for our metrics.
const (
	DCSKey      = attribute.Key("dcs")
	StateKey    = attribute.Key("state")
	ProviderKey = attribute.Key("provider")
	StatusKey   = attribute.Key("status")
	APP_PORT    = ":8088"
)
