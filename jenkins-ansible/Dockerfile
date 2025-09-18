FROM jenkins/jenkins:lts-jdk17

USER root

# Base tools (add python3-venv so we can create a venv)
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 python3-pip python3-venv \
    git curl unzip jq gnupg ca-certificates lsb-release \
  && rm -rf /var/lib/apt/lists/*

# ---- Ansible in a virtualenv (avoids PEP 668 "externally managed" error) ----
# Create venv, install Ansible + AWS libs, install amazon.aws collection,
# and expose binaries on PATH via symlinks.
RUN python3 -m venv /opt/ansible \
  && /opt/ansible/bin/pip install --no-cache-dir "ansible>=8,<9" boto3 botocore \
  && /opt/ansible/bin/ansible-galaxy collection install amazon.aws \
  && ln -s /opt/ansible/bin/ansible /usr/local/bin/ansible \
  && ln -s /opt/ansible/bin/ansible-playbook /usr/local/bin/ansible-playbook

# ---- AWS CLI v2 ----
RUN curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o /tmp/awscliv2.zip \
  && unzip /tmp/awscliv2.zip -d /tmp \
  && /tmp/aws/install \
  && rm -rf /tmp/aws /tmp/awscliv2.zip

# ---- GitHub CLI ----
RUN mkdir -p /etc/apt/keyrings \
  && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
     | tee /etc/apt/keyrings/githubcli-archive-keyring.gpg >/dev/null \
  && chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" \
     | tee /etc/apt/sources.list.d/github-cli.list >/dev/null \
  && apt-get update && apt-get install -y gh \
  && rm -rf /var/lib/apt/lists/*

# (optional) expose venv path for interactive shells
ENV PATH="/opt/ansible/bin:${PATH}"

USER jenkins
