{{- define "bank-web.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "bank-web.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "bank-web.labels" -}}
helm.sh/chart: {{ include "bank-web.chart" . }}
{{ include "bank-web.selectorLabels" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{- define "bank-web.selectorLabels" -}}
app.kubernetes.io/name: {{ include "bank-web.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}