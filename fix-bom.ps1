$base = "c:\Users\My-PC\Desktop\projects\wedding-planner\wedding-planner\src"
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$fixed = 0
Get-ChildItem -Path $base -Recurse -Filter "*.java" | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName, $utf8NoBom)
    $changed = $false
    if ($content.StartsWith([char]0xFEFF)) {
        $content = $content.Substring(1)
        $changed = $true
    }
    if ($content.StartsWith("ackage ")) {
        $content = "p" + $content
        $changed = $true
    }
    if ($changed) {
        [System.IO.File]::WriteAllText($_.FullName, $content, $utf8NoBom)
        Write-Host "Fixed: $($_.Name)"
        $fixed++
    }
}
Write-Host "Total fixed: $fixed"
