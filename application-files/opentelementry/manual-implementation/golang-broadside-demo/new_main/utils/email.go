package utils

import (
	"fmt"
	"log"
	"net"
	"strings"
)

func ConvertBMLStatus(raw []interface{}) map[string]int {
	result := make(map[string]int)
	for i := 0; i < len(raw); i += 2 {
		key, _ := raw[i].(string)
		value, _ := raw[i+1].(float64) // JSON numbers are decoded as float64
		result[key] = int(value)
	}
	return result
}

func ExtractStartAndEndTime(query string) (string, string) {

	start := strings.Index(query, "[")
	end := strings.Index(query, "]")

	if start != -1 && end != -1 {
		timeRange := query[start+1 : end] // Extracts 2025-05-25T00:00:00Z TO 2025-05-25T23:59:59Z
		parts := strings.Split(timeRange, " TO ")
		if len(parts) == 2 {
			fromTime := parts[0]
			toTime := parts[1]
			fmt.Println("From:", fromTime)
			fmt.Println("To  :", toTime)
			return fromTime, toTime
		}
	}
	return "", ""

}

// getOutboundIP attempts to determine the system's preferred outbound IP address.
func GetOutboundIP() string {
	conn, err := net.Dial("udp", "8.8.8.8:80") // Connect to a public DNS server (no data sent)
	if err != nil {
		log.Printf("Warning: Could not determine outbound IP. Using 'unknown_ip'. Error: %v", err)
		return "unknown_ip"
	}
	defer conn.Close() // Ensure the connection is closed

	localAddr := conn.LocalAddr().(*net.UDPAddr)
	return localAddr.IP.String()
}


