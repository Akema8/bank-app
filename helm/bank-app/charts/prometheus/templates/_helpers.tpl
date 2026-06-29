{{- define "prometheus.name" -}}
{{- "prometheus" }}
{{- end }}

{{- define "prometheus.labels" -}}
app: {{ include "prometheus.name" . }}
{{- end }}

{{- define "prometheus.selectorLabels" -}}
app: {{ include "prometheus.name" . }}
{{- end }}
