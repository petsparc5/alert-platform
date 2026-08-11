<#
.SYNOPSIS
    Renders a Claude Code session JSONL log into a readable Markdown transcript.
.DESCRIPTION
    Keeps user/assistant text turns (merging consecutive same-role records) and
    notes which tools the assistant used. Filters out queue operations, attachments,
    injected system-reminders, attachment references, and tool results.
#>
param(
    [int]$Part,
    [string]$InputPath,
    [string]$OutputPath,
    [string]$Title,
    [string]$ProjectLogDir = "C:\Users\petsp\.claude\projects\C--Users-petsp-IdeaProjects-Sonrisa",
    [string]$ConversationsDir = "C:\Users\petsp\IdeaProjects\Sonrisa\conversations"
)

if (-not $InputPath) {
    $latest = Get-ChildItem -LiteralPath $ProjectLogDir -Filter *.jsonl -ErrorAction Stop |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $latest) { throw "No .jsonl session logs found in $ProjectLogDir" }
    $InputPath = $latest.FullName
}

if (-not $OutputPath) {
    $suffix = if ($Part) { "_part$Part" } else { "_part1" }
    $OutputPath = Join-Path $ConversationsDir "implementation_with_claude$suffix.md"
}

if (-not $Title) {
    $partLabel = if ($Part) { " Part $Part" } else { "" }
    $Title = "Implementation Conversation$partLabel - Alert Platform (Sonrisa)"
}

$dir = Split-Path -Parent $OutputPath
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("# $Title")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Verbatim transcript exported from the Claude Code session log via ``export-transcript.ps1``.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("")

$lastWho = $null

foreach ($line in Get-Content -LiteralPath $InputPath -Encoding UTF8) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    try { $obj = $line | ConvertFrom-Json -ErrorAction Stop } catch { continue }
    if ($obj.type -ne 'user' -and $obj.type -ne 'assistant') { continue }

    $msg = $obj.message
    if ($null -eq $msg) { continue }
    $content = $msg.content

    $texts = @()
    $tools = @()

    if ($content -is [string]) {
        $texts += $content
    } else {
        foreach ($item in $content) {
            if ($item.type -eq 'text') {
                $t = $item.text
                if ($t -match '^\s*<system-reminder>') { }
                elseif ($t -match '^\s*\[@[^\]]+\]\(file:') { }
                else { $texts += $t }
            }
            elseif ($item.type -eq 'tool_use') {
                $tools += $item.name
            }
        }
    }

    if ($texts.Count -eq 0 -and $tools.Count -eq 0) { continue }

    $who = if ($msg.role -eq 'user') { 'Peter' } else { 'Claude' }

    if ($who -ne $lastWho) {
        [void]$sb.AppendLine("## $who")
        [void]$sb.AppendLine("")
        $lastWho = $who
    }

    foreach ($t in $texts) {
        [void]$sb.AppendLine($t.Trim())
        [void]$sb.AppendLine("")
    }

    if ($tools.Count -gt 0) {
        $uniq = ($tools | Select-Object -Unique) -join ', '
        [void]$sb.AppendLine("_[tools used: $uniq]_")
        [void]$sb.AppendLine("")
    }
}

Set-Content -LiteralPath $OutputPath -Value $sb.ToString() -Encoding utf8
Write-Output "Transcript written to $OutputPath"
