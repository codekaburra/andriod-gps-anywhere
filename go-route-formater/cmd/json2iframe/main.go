package main

import (
	"log"
	"os"

	"gps-anywhere-gps-formater/internal/config"
	"gps-anywhere-gps-formater/internal/pipeline"
)

func main() {
	cfg := config.JSON2IframeFromFlags()

	logger := log.New(os.Stdout, "", log.LstdFlags)
	if err := pipeline.ProcessAllJSON(cfg, logger); err != nil {
		logger.Fatalf("process failed: %v", err)
	}
}
