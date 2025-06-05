package model

type SolrResponse struct {
	ResponseHeader ResponseHeader `json:"responseHeader"`
	Response       Response       `json:"response"`
	FacetCounts    FacetCounts    `json:"facet_counts"`
}

type ResponseHeader struct {
	ZKConnected bool        `json:"zkConnected"`
	Status      int         `json:"status"`
	QTime       int         `json:"QTime"`
	Params      QueryParams `json:"params"`
}

type QueryParams struct {
	Query      string `json:"q"`
	FacetField string `json:"facet.field"`
	Start      string `json:"start"`
	Rows       string `json:"rows"`
	Facet      string `json:"facet"`
	WriterType string `json:"wt"`
}

type Response struct {
	NumFound      int           `json:"numFound"`
	Start         int           `json:"start"`
	MaxScore      float64       `json:"maxScore"`
	NumFoundExact bool          `json:"numFoundExact"`
	Docs          []interface{} `json:"docs"`
}

type FacetCounts struct {
	FacetQueries   map[string]interface{} `json:"facet_queries"`
	FacetFields    FacetFields            `json:"facet_fields"`
	FacetRanges    map[string]interface{} `json:"facet_ranges"`
	FacetIntervals map[string]interface{} `json:"facet_intervals"`
	FacetHeatmaps  map[string]interface{} `json:"facet_heatmaps"`
}

type FacetFields struct {
	BMLStatus []interface{} `json:"bml_status"`
}
