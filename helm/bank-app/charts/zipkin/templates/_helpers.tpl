{{- define "zipkin.name" -}}
{{- "zipkin" }}
{{- end }}

{{- define "zipkin.labels" -}}
app: {{ include "zipkin.name" . }}
{{- end }}

{{- define "zipkin.selectorLabels" -}}
app: {{ include "zipkin.name" . }}
{{- end }}
