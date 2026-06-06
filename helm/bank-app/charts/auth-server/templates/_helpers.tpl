{{- define "auth-server.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "auth-server.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "auth-server.labels" -}}
helm.sh/chart: {{ include "auth-server.chart" . }}
{{ include "auth-server.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "auth-server.selectorLabels" -}}
app.kubernetes.io/name: {{ include "auth-server.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}