<#
.SYNOPSIS
  海洋生物知识库 — 本地开发启动脚本 (PowerShell)
.DESCRIPTION
  分别启动后端 (Spring Boot) 和前端 (Vue 3)
  支持 start / stop / restart / status
.EXAMPLE
  .\start.ps1 start            启动前后端
  .\start.ps1 start backend    仅启动后端
  .\start.ps1 start frontend   仅启动前端
  .\start.ps1 stop             停止全部
  .\start.ps1 status           查看状态
#>

param(
    [ValidateSet('start','stop','restart','status')]
    [string]$Action = 'start',

    [ValidateSet('all','backend','frontend')]
    [string]$Module = 'all'
)

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$BackendDir = Join-Path $ProjectDir "ocean-server"
$FrontendDir = Join-Path $ProjectDir "ocean-web"
$BackendLog = Join-Path $BackendDir "ocean-server.log"

function Write-Info  { Write-Host "[INFO] $args" -ForegroundColor Cyan }
function Write-Ok   { Write-Host "[OK] $args" -ForegroundColor Green }
function Write-Warn { Write-Host "[WARN] $args" -ForegroundColor Yellow }
function Write-Err  { Write-Host "[ERROR] $args" -ForegroundColor Red }

# ====== 后端 ======

function Backend-Start {
    Write-Info "====== 启动后端 ======"

    # 检查端口
    $portInUse = netstat -ano | Select-String ":8080 " | Select-String "LISTENING"
    if ($portInUse) {
        Write-Warn "端口 8080 已被占用，后端可能已在运行"
        Write-Host "  执行 .\start.ps1 stop backend 后再试"
        return
    }

    # 编译
    Write-Host "[1/2] 编译打包..." -ForegroundColor Yellow
    Push-Location $BackendDir
    mvn clean package -DskipTests -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "编译失败"
        Pop-Location
        exit 1
    }
    Write-Ok "编译成功"
    Pop-Location

    # 启动
    Write-Host "[2/2] 启动服务..." -ForegroundColor Yellow
    $jarFile = Join-Path $BackendDir "target\ocean-server-1.0.0.jar"
    $javaExe = if (Get-Command "java" -ErrorAction SilentlyContinue) { "java" }
               else { "${env:JAVA_HOME}\bin\java.exe" }

    if (!(Test-Path $javaExe)) {
        Write-Err "未找到 Java，请安装 JDK 1.8+ 或设置 JAVA_HOME"
        exit 1
    }

    $proc = Start-Process -FilePath $javaExe -ArgumentList "-jar `"$jarFile`"" -NoNewWindow -PassThru -RedirectStandardOutput $BackendLog

    # 等待启动
    Write-Host "  等待启动" -NoNewline
    $timeout = 60
    $elapsed = 0
    while ($elapsed -lt $timeout) {
        Start-Sleep -Seconds 2
        $elapsed += 2
        Write-Host "." -NoNewline

        if (Select-String -Path $BackendLog -Pattern "Started OceanApplication" -Quiet) {
            Write-Host ""
            Write-Ok "后端启动成功! PID: $($proc.Id)"
            Write-Host "  API:     http://localhost:8080" -ForegroundColor Cyan
            Write-Host "  Swagger: http://localhost:8080/doc.html" -ForegroundColor Cyan
            return
        }
        if (Select-String -Path $BackendLog -Pattern "Application run failed" -Quiet) {
            Write-Host ""
            Write-Err "启动失败，查看日志:"
            Get-Content $BackendLog -Tail 20
            exit 1
        }
    }
    Write-Host ""
    Write-Err "启动超时，请检查日志: Get-Content $BackendLog -Tail 20"
}

function Backend-Stop {
    Write-Info "====== 停止后端 ======"
    $portProc = netstat -ano | Select-String ":8080 " | Select-String "LISTENING"
    if ($portProc) {
        $pid = ($portProc -split '\s+')[-1]
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        Write-Ok "后端已停止 (PID: $pid)"
    } else {
        Write-Warn "后端未在运行"
    }
}

function Backend-Status {
    $portProc = netstat -ano | Select-String ":8080 " | Select-String "LISTENING"
    if ($portProc) {
        $pid = ($portProc -split '\s+')[-1]
        Write-Host "  [RUNNING] 后端     PID: $pid    http://localhost:8080" -ForegroundColor Green
    } else {
        Write-Host "  [STOPPED] 后端" -ForegroundColor Red
    }
}

# ====== 前端 ======

function Frontend-Start {
    Write-Info "====== 启动前端 ======"

    if (!(Get-Command "node" -ErrorAction SilentlyContinue)) {
        Write-Err "未找到 Node.js，请安装 Node.js 16+"
        exit 1
    }

    # 检查端口
    $portInUse = netstat -ano | Select-String ":3000 " | Select-String "LISTENING"
    if ($portInUse) {
        Write-Warn "端口 3000 已被占用"
        Write-Host "  执行 .\start.ps1 stop frontend 后再试"
        return
    }

    Push-Location $FrontendDir

    # 安装依赖
    if (!(Test-Path "node_modules")) {
        Write-Host "[1/2] 安装依赖..." -ForegroundColor Yellow
        npm install 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Err "依赖安装失败"
            Pop-Location
            exit 1
        }
        Write-Ok "依赖安装完成"
    } else {
        Write-Host "[1/2] 依赖已就绪" -ForegroundColor Yellow
    }

    # 启动开发服务器
    Write-Host "[2/2] 启动开发服务器..." -ForegroundColor Yellow
    $env:BROWSER = "none"
    $proc = Start-Process -FilePath "npx.cmd" -ArgumentList "vue-cli-service serve" -NoNewWindow -PassThru

    Start-Sleep -Seconds 6
    if (!$proc.HasExited) {
        Write-Ok "前端启动成功! PID: $($proc.Id)"
        Write-Host "  地址: http://localhost:3000" -ForegroundColor Cyan
    } else {
        Write-Err "前端启动失败"
    }
    Pop-Location
}

function Frontend-Stop {
    Write-Info "====== 停止前端 ======"
    $portProc = netstat -ano | Select-String ":3000 " | Select-String "LISTENING"
    if ($portProc) {
        $pid = ($portProc -split '\s+')[-1]
        Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
        Write-Ok "前端已停止 (PID: $pid)"
    } else {
        Write-Warn "前端未在运行"
    }
}

function Frontend-Status {
    $portProc = netstat -ano | Select-String ":3000 " | Select-String "LISTENING"
    if ($portProc) {
        $pid = ($portProc -split '\s+')[-1]
        Write-Host "  [RUNNING] 前端     PID: $pid    http://localhost:3000" -ForegroundColor Green
    } else {
        Write-Host "  [STOPPED] 前端" -ForegroundColor Red
    }
}

# ====== 基础设施检查 ======

function Check-Infra {
    Write-Host "`n检查基础设施..." -ForegroundColor Yellow

    # MySQL
    if (Get-Command "mysql" -ErrorAction SilentlyContinue) {
        $result = mysql -u root -p"${env:MYSQL_PASSWORD}" -e "SELECT 1" 2>&1
        if ($LASTEXITCODE -eq 0) {
            Write-Ok "MySQL       运行中"
        } else {
            Write-Warn "MySQL      未连接 (检查 MYSQL_PASSWORD 环境变量)"
        }
    }

    # Redis
    if (Get-Command "redis-cli" -ErrorAction SilentlyContinue) {
        $pong = redis-cli ping 2>&1
        if ($pong -eq "PONG") {
            Write-Ok "Redis       运行中"
        } else {
            Write-Warn "Redis       未启动"
        }
    }

    # ES
    try {
        $esResp = Invoke-WebRequest -Uri "http://localhost:9200" -UseBasicParsing -TimeoutSec 3
        Write-Ok "ElasticSearch 运行中"
    } catch {
        Write-Warn "ES         未启动 (搜索功能暂不可用)"
    }
}

# ====== 主流程 ======

switch ($Action) {
    "start" {
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  海洋生物知识库 — 启动" -ForegroundColor Cyan
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan

        if ($Module -eq "all" -or $Module -eq "backend") {
            Check-Infra
        }

        switch ($Module) {
            "all" {
                Backend-Start
                Write-Host ""
                Frontend-Start
                Write-Host ""
                Write-Host "══════════════════════════════════════════" -ForegroundColor Green
                Write-Host "  🎉 全部启动完成!" -ForegroundColor Green
                Write-Host "  前端: http://localhost:3000" -ForegroundColor Cyan
                Write-Host "  后端: http://localhost:8080" -ForegroundColor Cyan
                Write-Host "  Swagger: http://localhost:8080/doc.html" -ForegroundColor Cyan
                Write-Host "══════════════════════════════════════════" -ForegroundColor Green
            }
            "backend" { Backend-Start }
            "frontend" { Frontend-Start }
        }
    }
    "stop" {
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  停止服务" -ForegroundColor Cyan
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
        switch ($Module) {
            "all" {
                Backend-Stop
                Frontend-Stop
                Write-Ok "已全部停止"
            }
            "backend" { Backend-Stop }
            "frontend" { Frontend-Stop }
        }
    }
    "restart" {
        Write-Host "====== 重启 ======" -ForegroundColor Cyan
        switch ($Module) {
            "all" {
                Backend-Stop; Start-Sleep 2
                Frontend-Stop; Start-Sleep 2
                Backend-Start; Write-Host ""
                Frontend-Start
                Write-Ok "重启完成"
            }
            "backend" { Backend-Stop; Start-Sleep 2; Backend-Start }
            "frontend" { Frontend-Stop; Start-Sleep 2; Frontend-Start }
        }
    }
    "status" {
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host "  服务状态" -ForegroundColor Cyan
        Write-Host "══════════════════════════════════════════" -ForegroundColor Cyan
        Write-Host ""
        Backend-Status
        Frontend-Status
    }
}
