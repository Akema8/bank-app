{{- define "cash.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "cash.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "cash.labels" -}}
helm.sh/chart: {{ include "cash.chart" . }}
{{ include "cash.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "cash.selectorLabels" -}}
app.kubernetes.io/name: {{ include "cash.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}