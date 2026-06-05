package config

import "flag"

const (
	DefaultTodoDir   = "input/todo"
	DefaultResultDir = "output"
	DefaultDoneDir   = "input/done"
)

type Config struct {
	TodoDir   string
	ResultDir string
	DoneDir   string
}

func FromFlags() Config {
	todoDir := flag.String("todo-dir", DefaultTodoDir, "directory containing source iframe html files")
	resultDir := flag.String("result-dir", DefaultResultDir, "directory where converted json files are written")
	doneDir := flag.String("done-dir", DefaultDoneDir, "directory where processed iframe files are moved")
	flag.Parse()

	return Config{
		TodoDir:   *todoDir,
		ResultDir: *resultDir,
		DoneDir:   *doneDir,
	}
}
