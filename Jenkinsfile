pipeline {
  agent any

  environment {
    AWS_REGION = 'ap-southeast-2'
    AWS_CREDS  = 'aws-creds'   // Jenkins credentials ID with ECR/ECS/IAM perms
    ECR_REPO   = 'myapp-web'   // your ECR repo name
  }

  stages {

    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Resolve latest ECR image') {
      steps {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: env.AWS_CREDS]]) {
          sh '''#!/bin/bash
            set -euo pipefail
            export AWS_DEFAULT_REGION="$AWS_REGION"

            ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
            REGISTRY="${ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com"

            echo "Resolving latest image in ${ECR_REPO} …"
            IMAGE_DIGEST=$(aws ecr describe-images \
              --repository-name "$ECR_REPO" \
              --query 'reverse(sort_by(imageDetails,& imagePushedAt))[0].imageDigest' \
              --output text)

            if [[ -z "$IMAGE_DIGEST" || "$IMAGE_DIGEST" == "None" ]]; then
              echo "No images found in ECR repo $ECR_REPO"
              exit 1
            fi

            IMAGE_URI="${REGISTRY}/${ECR_REPO}@${IMAGE_DIGEST}"
            echo "Resolved latest image: $IMAGE_URI"

            mkdir -p artifacts
            echo "IMAGE_URI=$IMAGE_URI" > artifacts/deploy.env
            cat artifacts/deploy.env
          '''
        }
      }
    }

    stage('Deploy to ECS with Ansible') {
    steps {
      withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: env.AWS_CREDS]]) {
      sh '''#!/bin/bash
        set -euo pipefail
        export AWS_DEFAULT_REGION="$AWS_REGION"

        # Reinstall collections with versions compatible with ansible-core 2.15
        ansible-galaxy collection install -r ansible/requirements.yml \
          -p .ansible/collections --force

        # Search the workspace first, then global paths
        export ANSIBLE_COLLECTIONS_PATHS="$PWD/.ansible/collections:/usr/share/ansible/collections:/var/jenkins_home/.ansible/collections"

        ansible-playbook -i ansible/inventory ansible/deploy-ecs.yml \
                --extra-vars "image_uri=$IMAGE_URI \
                  exec_role_arn=arn:aws:iam::988360983746:role/ecsTaskExecutionRole \
                  task_role_arn=arn:aws:iam::988360983746:role/myappTaskRole"
                '''
    }
  }
}
  }
}
