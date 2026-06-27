{{- define "notifications.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "notifications.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "notifications.labels" -}}
helm.sh/chart: {{ include "notifications.chart" . }}
{{ include "notifications.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "notifications.selectorLabels" -}}
app.kubernetes.io/name: {{ include "notifications.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}