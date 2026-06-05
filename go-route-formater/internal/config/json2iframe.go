package config

import "flag"

const (
	DefaultJSONInputDir   = "cmd/json2iframe/input"
	DefaultHTMLOutputDir  = "cmd/json2iframe/output"
	DefaultIframeWidth    = 600
	DefaultIframeHeight   = 450
	DefaultIframeLoading  = "lazy"
	DefaultIframeReferrer = "no-referrer-when-downgrade"
)

type JSON2IframeConfig struct {
	InputDir       string
	OutputDir      string
	Once           bool
	Width          int
	Height         int
	Loading        string
	ReferrerPolicy string
}

func JSON2IframeFromFlags() JSON2IframeConfig {
	inputDir := flag.String("input-dir", DefaultJSONInputDir, "directory containing route json files")
	outputDir := flag.String("output-dir", DefaultHTMLOutputDir, "directory where generated iframe html files are written")
	once := flag.Bool("once", false, "process only one json file and move it to done directory")
	width := flag.Int("width", DefaultIframeWidth, "iframe width attribute")
	height := flag.Int("height", DefaultIframeHeight, "iframe height attribute")
	loading := flag.String("loading", DefaultIframeLoading, "iframe loading attribute")
	referrerPolicy := flag.String("referrerpolicy", DefaultIframeReferrer, "iframe referrerpolicy attribute")
	flag.Parse()

	return JSON2IframeConfig{
		InputDir:       *inputDir,
		OutputDir:      *outputDir,
		Once:           *once,
		Width:          *width,
		Height:         *height,
		Loading:        *loading,
		ReferrerPolicy: *referrerPolicy,
	}
}
