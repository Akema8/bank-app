{{- define "accounts.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "accounts.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "accounts.labels" -}}
helm.sh/chart: {{ include "accounts.chart" . }}
{{ include "accounts.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "accounts.selectorLabels" -}}
app.kubernetes.io/name: {{ include "accounts.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}