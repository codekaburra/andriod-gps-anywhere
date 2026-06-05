package main

import (
	"log"
	"os"

	"gps-anywhere-gps-formater/internal/config"
	"gps-anywhere-gps-formater/internal/pipeline"
)

func main() {
	cfg := config.FromFlags()

	logger := log.New(os.Stdout, "", log.LstdFlags)
	if err := pipeline.ProcessAll(cfg, logger); err != nil {
		logger.Fatalf("process failed: %v", err)
	}
}
