package parser

import (
	"encoding/base64"
	"errors"
	"fmt"
	"net/url"
	"regexp"
	"strconv"
	"strings"

	"gps-anywhere-gps-formater/internal/model"
)

var iframeSrcRegex = regexp.MustCompile(`(?is)<iframe[^>]*\ssrc\s*=\s*["']([^"']+)["'][^>]*>`)

func ExtractIframeSrc(html string) (string, error) {
	matches := iframeSrcRegex.FindStringSubmatch(html)
	if len(matches) < 2 {
		return "", errors.New("iframe src not found")
	}

	return strings.TrimSpace(matches[1]), nil
}

func ParseCoordinatesFromIframeSrc(src string) ([]model.Coordinate, error) {
	parsedURL, err := url.Parse(src)
	if err != nil {
		return nil, fmt.Errorf("invalid iframe src url: %w", err)
	}

	pb := parsedURL.Query().Get("pb")
	if pb == "" {
		return nil, errors.New("missing pb query parameter")
	}

	return ParseCoordinatesFromPB(pb)
}

func ParseCoordinatesFromPB(pb string) ([]model.Coordinate, error) {
	tokens := strings.Split(pb, "!")

	coordinates := make([]model.Coordinate, 0)
	var currentName string
	var currentLat *float64

	for _, token := range tokens {
		if token == "" {
			continue
		}

		switch {
		case strings.HasPrefix(token, "2z"):
			currentName = decodeNameToken(token[2:])
			if currentName == "" {
				continue
			}
			currentLat = nil
		case strings.HasPrefix(token, "1d"):
			if currentName == "" {
				continue
			}
			lat, err := strconv.ParseFloat(token[2:], 64)
			if err != nil {
				continue
			}
			currentLat = &lat
		case strings.HasPrefix(token, "2d"):
			if currentName == "" || currentLat == nil {
				continue
			}
			lon, err := strconv.ParseFloat(token[2:], 64)
			if err != nil {
				continue
			}
			coordinates = append(coordinates, model.Coordinate{
				Name:      currentName,
				Latitude:  *currentLat,
				Longitude: lon,
			})
			currentName = ""
			currentLat = nil
		}
	}

	if len(coordinates) == 0 {
		return nil, errors.New("no coordinates parsed from pb data")
	}

	return coordinates, nil
}

func decodeNameToken(value string) string {
	raw, err := url.QueryUnescape(value)
	if err != nil {
		raw = value
	}

	name := strings.TrimSpace(raw)
	if name == "" {
		return ""
	}

	// Google map place names inside `2z` are frequently url-safe base64 strings.
	if decoded, err := base64.RawURLEncoding.DecodeString(name); err == nil {
		name = strings.TrimSpace(string(decoded))
	}

	// Many embeds append address after a space, e.g. "Place Name 700CityAddress".
	parts := strings.SplitN(name, " ", 2)
	return strings.TrimSpace(parts[0])
}
