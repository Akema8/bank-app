{{- define "grafana.name" -}}
{{- "grafana" }}
{{- end }}

{{- define "grafana.labels" -}}
app: {{ include "grafana.name" . }}
{{- end }}

{{- define "grafana.selectorLabels" -}}
app: {{ include "grafana.name" . }}
{{- end }}
