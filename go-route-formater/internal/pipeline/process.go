package pipeline

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"gps-anywhere-gps-formater/internal/config"
	"gps-anywhere-gps-formater/internal/model"
	"gps-anywhere-gps-formater/internal/parser"
)

func ProcessAll(cfg config.Config, logger *log.Logger) error {
	if err := ensureDirectories(cfg); err != nil {
		return err
	}

	inputFiles, err := listHTMLFiles(cfg.InputDir)
	if err != nil {
		return fmt.Errorf("failed to list input files: %w", err)
	}
	if len(inputFiles) == 0 {
		logger.Printf("no html files found in %s", cfg.InputDir)
		return nil
	}

	if cfg.Once {
		if err := processSingleFile(inputFiles[0], cfg, logger, true); err != nil {
			return fmt.Errorf("process failed for %s: %w", filepath.Base(inputFiles[0]), err)
		}
		return nil
	}

	for _, inputPath := range inputFiles {
		if err := processSingleFile(inputPath, cfg, logger, false); err != nil {
			logger.Printf("skip %s: %v", filepath.Base(inputPath), err)
			continue
		}
	}

	return nil
}

func ensureDirectories(cfg config.Config) error {
	dirs := []string{cfg.InputDir, cfg.OutputDir}
	if cfg.Once {
		dirs = append(dirs, doneDirFromInput(cfg.InputDir))
	}
	for _, dir := range dirs {
		if err := os.MkdirAll(dir, 0o755); err != nil {
			return fmt.Errorf("failed to create directory %s: %w", dir, err)
		}
	}
	return nil
}

func listHTMLFiles(todoDir string) ([]string, error) {
	entries, err := os.ReadDir(todoDir)
	if err != nil {
		return nil, err
	}

	files := make([]string, 0)
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		if strings.EqualFold(filepath.Ext(entry.Name()), ".html") {
			files = append(files, filepath.Join(todoDir, entry.Name()))
		}
	}

	sort.Strings(files)
	return files, nil
}

func processSingleFile(inputPath string, cfg config.Config, logger *log.Logger, moveProcessed bool) error {
	content, err := os.ReadFile(inputPath)
	if err != nil {
		return fmt.Errorf("read failed: %w", err)
	}

	src, err := parser.ExtractIframeSrc(string(content))
	if err != nil {
		return fmt.Errorf("extract iframe src failed: %w", err)
	}

	coords, err := parser.ParseCoordinatesFromIframeSrc(src)
	if err != nil {
		return fmt.Errorf("parse coordinates failed: %w", err)
	}

	base := strings.TrimSuffix(filepath.Base(inputPath), filepath.Ext(inputPath))
	outputName, timestampID, err := buildTimestampOutputName(base, cfg.OutputDir)
	if err != nil {
		return fmt.Errorf("build output filename failed: %w", err)
	}

	route := model.Route{
		RouteID:     timestampID,
		RouteName:   base,
		Version:     1,
		Coordinates: coords,
	}

	outputPath := filepath.Join(cfg.OutputDir, outputName)
	if err := writeRouteJSON(outputPath, route); err != nil {
		return fmt.Errorf("write json failed: %w", err)
	}

	if moveProcessed {
		if err := moveToDone(inputPath, doneDirFromInput(cfg.InputDir)); err != nil {
			return fmt.Errorf("move source to done failed: %w", err)
		}
	}

	moveStatus := "kept in input"
	if moveProcessed {
		moveStatus = "moved to done"
	}
	logger.Printf("processed %s -> %s (%d points, %s)", filepath.Base(inputPath), outputName, len(coords), moveStatus)
	return nil
}

func buildTimestampOutputName(base, resultDir string) (string, string, error) {
	// Timestamp format: YYYYMMDDHHmmSSS (14 date/time chars + 3 millis = 17 chars total).
	for i := 0; i < 1000; i++ {
		now := time.Now()
		timestamp := now.Format("20060102150405") + fmt.Sprintf("%03d", now.Nanosecond()/1e6)
		filename := fmt.Sprintf("%s_%s.json", base, timestamp)
		path := filepath.Join(resultDir, filename)
		if _, err := os.Stat(path); os.IsNotExist(err) {
			return filename, timestamp, nil
		} else if err != nil {
			return "", "", err
		}

		time.Sleep(time.Millisecond)
	}

	return "", "", fmt.Errorf("unable to allocate unique output name for %s", base)
}

func doneDirFromInput(inputDir string) string {
	return filepath.Join(inputDir, "done")
}

func writeRouteJSON(outputPath string, route model.Route) error {
	data, err := json.MarshalIndent(route, "", "  ")
	if err != nil {
		return err
	}
	data = append(data, '\n')
	return os.WriteFile(outputPath, data, 0o644)
}

func moveToDone(inputPath, doneDir string) error {
	destination := filepath.Join(doneDir, filepath.Base(inputPath))
	if _, err := os.Stat(destination); err == nil {
		base := strings.TrimSuffix(filepath.Base(inputPath), filepath.Ext(inputPath))
		ext := filepath.Ext(inputPath)
		destination = filepath.Join(doneDir, fmt.Sprintf("%s_%d%s", base, time.Now().Unix(), ext))
	}
	return os.Rename(inputPath, destination)
}
