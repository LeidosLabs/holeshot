Set-ExecutionPolicy Bypass -Scope Process -Force; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))
choco install awscli -y
choco install jq -y
choco install jdk8 -y -params "source=false"
choco install cygwin -y
choco install ant -y -i
choco install maven -y
choco install git -y -params "/GitAndUnixToolsOnPath /NoGitLfs /SChannel /NoAutoCrlf"
