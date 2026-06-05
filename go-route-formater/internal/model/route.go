package model

type Coordinate struct {
	Name      string  `json:"name"`
	Latitude  float64 `json:"latitude"`
	Longitude float64 `json:"longitude"`
}

type Route struct {
	RouteID     string       `json:"route_id"`
	RouteName   string       `json:"route_name"`
	Version     int          `json:"version"`
	Coordinates []Coordinate `json:"coordinates"`
}
