## Hashicorp-kv-learning

Create the vault namespace:

```bash
kubectl create namespace vault
```

Add the HashiCorp Helm repository

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
```

Install HashiCorp Vault using Helm:

```bash
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault \
    --set="server.dev.enabled=true" \
    --set="ui.enabled=true" \
    --set="ui.serviceType=NodePort" \
    --namespace vault
```

Wait for the vault pod to be ready:

```bash
kubectl wait --for=condition=Ready pod/vault-0 -n vault
```

Enter inside the vault pod to configure vault with kubernetes

```bash
kubectl exec -it vault-0 -n vault -- /bin/sh
```

Create a policy for reading secrets (read-policy.hcl):

```bash
cat <<EOF > /home/vault/read-policy.hcl
path "secret*" {
    capabilities = ["read"]
}
EOF
```

Write the policy to Vault:

```bash
vault policy write read-policy /home/vault/read-policy.hcl
```

Enable Kubernetes authentication in Vault:

```bash
vault auth enable kubernetes
```

Configure Vault to communicate with the Kubernetes API server

```bash
vault write auth/kubernetes/config \
    token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
    kubernetes_host="https://${KUBERNETES_PORT_443_TCP_ADDR}:443" \
    kubernetes_ca_cert=@/var/run/secrets/kubernetes.io/serviceaccount/ca.crt
```

Create a role(vault-role) that binds the above policy to a Kubernetes service account(vault-serviceaccount) in a specific namespace. This allows the service account to access secrets stored in Vault

```bash
vault write auth/kubernetes/role/vault-role \
     bound_service_account_names=vault-serviceaccount \
     bound_service_account_namespaces=vault \
     policies=read-policy \
     ttl=1h
```

Create secret using

```bash
vault kv put secret/clisecret token=asecret
```

Verify if secret created or not

```bash
vault kv list secret
```



To deploy the application and inject the secret you need to include the necessary annotations in your deployment file, you can refer the following example code snippet:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: your-app-deployment
  annotations:
    vault.hashicorp.com/agent-inject: "true"
    vault.hashicorp.com/agent-inject-secret-clisecret: "secret/clisecret"
    vault.hashicorp.com/agent-inject-template-clisecret: |
      {{- with secret "secret/clisecret" -}}
      token={{ .Data.token }}
      {{- end -}}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: your-app
  template:
    metadata:
      labels:
        app: your-app
    spec:
      containers:
        - name: your-app-container
          image: your-app-image
          ports:
            - containerPort: 8080
          env:
            - name: TOKEN
              valueFrom:
                secretKeyRef:
                  name: your-app-secrets
                  key: token
```

Make sure to replace `your-app-deployment`, `your-app`, `your-app-container`, `your-app-image`, and `your-app-secrets` with your own values.



Check data if secret is injected or not in the pod

```bash
kubectl exec -it <pod name> -n vault -- ls /vault/secrets/
kubectl exec -it <pod name> -n vault -- cat /vault/secrets/clisecret
kubectl exec -it <pod name> -n vault -- cat /vault/secrets/uisecret
```