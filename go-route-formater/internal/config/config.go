package config

import "flag"

const (
	DefaultInputDir  = "cmd/iframe2json/input"
	DefaultOutputDir = "cmd/iframe2json/output"
)

type Config struct {
	InputDir  string
	OutputDir string
	Once      bool
}

func FromFlags() Config {
	inputDir := flag.String("input-dir", DefaultInputDir, "directory containing source iframe html files")
	outputDir := flag.String("output-dir", DefaultOutputDir, "directory where converted json files are written")
	once := flag.Bool("once", false, "process only one html file and move it to done directory")
	flag.Parse()

	return Config{
		InputDir:  *inputDir,
		OutputDir: *outputDir,
		Once:      *once,
	}
}
