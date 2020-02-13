docker build -t codebuild-win:latest -m 2GB .
Invoke-Expression -Command (Get-ECRLoginCommand -Region us-east-1).Command
docker tag codebuild-win:latest 199974664221.dkr.ecr.us-east-1.amazonaws.com/codebuild-win:latest
docker push 199974664221.dkr.ecr.us-east-1.amazonaws.com/codebuild-win:latest