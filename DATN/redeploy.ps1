<#
.SYNOPSIS
    Fast redeploy script for DATN microservices.

.DESCRIPTION
    Rebuilds the Maven project (JAR) and then rebuilds/restarts the Docker container.
    Usage: .\redeploy.ps1 <service-name>
    Example: .\redeploy.ps1 user-service
    Example: .\redeploy.ps1 project

.PARAMETER Service
    The name of the service to redeploy (e.g., user-service, project, api-gateway).
#>

param (
    [Parameter(Mandatory=$true)]
    [string]$Service
)

# 1. Map simple names to Directory and Container Name
$ServiceMap = @{
    "user-service"         = @{ Dir = "BE\UserService"; Container = "user-service" }
    "user"                 = @{ Dir = "BE\UserService"; Container = "user-service" }
    
    "project-service"      = @{ Dir = "BE\ProjectService"; Container = "project-service" }
    "project"              = @{ Dir = "BE\ProjectService"; Container = "project-service" }
    
    "scheduling-service"   = @{ Dir = "BE\SchedulingService"; Container = "scheduling-service" }
    "scheduling"           = @{ Dir = "BE\SchedulingService"; Container = "scheduling-service" }
    
    "document-service"     = @{ Dir = "BE\DocumentService"; Container = "document-service" }
    "document"             = @{ Dir = "BE\DocumentService"; Container = "document-service" }
    
    "forum-service"        = @{ Dir = "BE\ForumService"; Container = "forum-service" }
    "forum"                = @{ Dir = "BE\ForumService"; Container = "forum-service" }
    
    "notification-service" = @{ Dir = "BE\NotificationService"; Container = "notification-service" }
    "notification"         = @{ Dir = "BE\NotificationService"; Container = "notification-service" }
    
    "api-gateway"          = @{ Dir = "BE\APIGatewayService"; Container = "api-gateway" }
    "gateway"              = @{ Dir = "BE\APIGatewayService"; Container = "api-gateway" }

    "collab-server"        = @{ Dir = "collab-server"; Container = "collab-server"; Type = "node" }
    "collab"               = @{ Dir = "collab-server"; Container = "collab-server"; Type = "node" }
}

$Config = $ServiceMap[$Service]

if (-not $Config) {
    Write-Host "❌ Unknown service: $Service" -ForegroundColor Red
    Write-Host "Available options: user, project, scheduling, document, forum, notification, gateway, collab"
    exit 1
}

$RootDir = Get-Location
$ServiceDir = Join-Path $RootDir $Config.Dir

# 2. Build the Code
Write-Host "🚀 part 1: Building code for $($Config.Container)..." -ForegroundColor Cyan

if ($Config.Type -eq "node") {
    Write-Host "   (Node.js project - Run 'npm install' if needed, otherwise skipping build step)" -ForegroundColor Gray
} else {
    # Java/Maven Build
    Push-Location $ServiceDir
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "❌ Maven build failed!" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
}

# 3. Rebuild & Restart Docker Container
Write-Host "🐳 Part 2: Redeploying Container..." -ForegroundColor Cyan
docker-compose -f docker-compose.services.yml up -d --build --force-recreate $Config.Container

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Successfully redeployed $($Config.Container)!" -ForegroundColor Green
} else {
    Write-Host "❌ Docker deployment failed!" -ForegroundColor Red
}
