{{- define "transfer.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "transfer.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "transfer.labels" -}}
helm.sh/chart: {{ include "transfer.chart" . }}
{{ include "transfer.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "transfer.selectorLabels" -}}
app.kubernetes.io/name: {{ include "transfer.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}