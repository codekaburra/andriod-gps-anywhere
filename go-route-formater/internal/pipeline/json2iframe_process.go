package pipeline

import (
	"encoding/json"
	"fmt"
	"html"
	"log"
	"net/url"
	"os"
	"path/filepath"
	"sort"
	"strings"

	"gps-anywhere-gps-formater/internal/config"
	"gps-anywhere-gps-formater/internal/model"
)

func ProcessAllJSON(cfg config.JSON2IframeConfig, logger *log.Logger) error {
	if err := ensureJSON2IframeDirs(cfg); err != nil {
		return err
	}

	inputFiles, err := listJSONFiles(cfg.InputDir)
	if err != nil {
		return fmt.Errorf("failed to list input files: %w", err)
	}
	if len(inputFiles) == 0 {
		logger.Printf("no json files found in %s", cfg.InputDir)
		return nil
	}

	if cfg.Once {
		if err := processSingleJSONFile(inputFiles[0], cfg, logger, true); err != nil {
			return fmt.Errorf("process failed for %s: %w", filepath.Base(inputFiles[0]), err)
		}
		return nil
	}

	for _, inputPath := range inputFiles {
		if err := processSingleJSONFile(inputPath, cfg, logger, false); err != nil {
			logger.Printf("skip %s: %v", filepath.Base(inputPath), err)
			continue
		}
	}

	return nil
}

func ensureJSON2IframeDirs(cfg config.JSON2IframeConfig) error {
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

func listJSONFiles(inputDir string) ([]string, error) {
	entries, err := os.ReadDir(inputDir)
	if err != nil {
		return nil, err
	}

	files := make([]string, 0)
	for _, entry := range entries {
		if entry.IsDir() {
			continue
		}
		if strings.EqualFold(filepath.Ext(entry.Name()), ".json") {
			files = append(files, filepath.Join(inputDir, entry.Name()))
		}
	}

	sort.Strings(files)
	return files, nil
}

func processSingleJSONFile(inputPath string, cfg config.JSON2IframeConfig, logger *log.Logger, moveProcessed bool) error {
	content, err := os.ReadFile(inputPath)
	if err != nil {
		return fmt.Errorf("read failed: %w", err)
	}

	var route model.Route
	if err := json.Unmarshal(content, &route); err != nil {
		return fmt.Errorf("invalid route json: %w", err)
	}
	if len(route.Coordinates) == 0 {
		return fmt.Errorf("route has no coordinates")
	}

	iframeSrc := buildIframeSrcFromCoordinates(route.Coordinates)
	iframeHTML := buildIframeHTML(iframeSrc, cfg)

	base := strings.TrimSuffix(filepath.Base(inputPath), filepath.Ext(inputPath))
	outputName := base + ".html"
	outputPath := filepath.Join(cfg.OutputDir, outputName)
	if err := os.WriteFile(outputPath, []byte(iframeHTML+"\n"), 0o644); err != nil {
		return fmt.Errorf("write html failed: %w", err)
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
	logger.Printf("processed %s -> %s (%d points, %s)", filepath.Base(inputPath), outputName, len(route.Coordinates), moveStatus)
	return nil
}

func buildIframeSrcFromCoordinates(coords []model.Coordinate) string {
	var pb strings.Builder
	for _, coord := range coords {
		name := strings.TrimSpace(coord.Name)
		encodedName := url.QueryEscape(name)
		if name == "" {
			encodedName = "Point"
		}
		pb.WriteString("!2z")
		pb.WriteString(encodedName)
		pb.WriteString("!1d")
		pb.WriteString(fmt.Sprintf("%.15g", coord.Latitude))
		pb.WriteString("!2d")
		pb.WriteString(fmt.Sprintf("%.15g", coord.Longitude))
	}

	return "https://www.google.com/maps/embed?pb=" + pb.String()
}

func buildIframeHTML(src string, cfg config.JSON2IframeConfig) string {
	return fmt.Sprintf(
		`<iframe src="%s" width="%d" height="%d" style="border:0;" allowfullscreen="" loading="%s" referrerpolicy="%s"></iframe>`,
		html.EscapeString(src),
		cfg.Width,
		cfg.Height,
		html.EscapeString(cfg.Loading),
		html.EscapeString(cfg.ReferrerPolicy),
	)
}
