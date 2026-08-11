<#
.SYNOPSIS
    Renders a Claude Code session JSONL log into a readable Markdown transcript.
.DESCRIPTION
    Keeps user/assistant text turns (merging consecutive same-role records) and
    notes which tools the assistant used. Filters out queue operations, attachments,
    injected system-reminders, attachment references, and tool results.
#>
param(
    [string]$InputPath  = "C:\Users\petsp\.claude\projects\C--Users-petsp-IdeaProjects-Sonrisa\b9a1c1f2-9895-4099-9458-953caba86942.jsonl",
    [string]$OutputPath = "C:\Users\petsp\IdeaProjects\Sonrisa\conversations\design_with_claude.md"
)

$dir = Split-Path -Parent $OutputPath
if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

$sb = New-Object System.Text.StringBuilder
[void]$sb.AppendLine("# Design Conversation - Alert Platform (Sonrisa)")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("Verbatim transcript exported from the Claude Code session log via ``export-transcript.ps1``.")
[void]$sb.AppendLine("")
[void]$sb.AppendLine("---")
[void]$sb.AppendLine("")

$lastWho = $null

foreach ($line in Get-Content -LiteralPath $InputPath) {
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
