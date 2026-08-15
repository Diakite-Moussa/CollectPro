#!/bin/bash

# Configuration
# Charge les variables depuis le .env de production si present, pour rester
# synchronise avec les identifiants reellement utilises par docker-compose
# (DB_USERNAME peut differer de la valeur par defaut "collectepro_user").
ENV_FILE="$(dirname "$0")/../.env"
if [ -f "$ENV_FILE" ]; then
    set -a
    source "$ENV_FILE"
    set +a
fi

BACKUP_DIR="/var/backups/collectpro"
DATE=$(date +%Y%m%d_%H%M%S)
CONTAINER_NAME="collectpro_db_prod"
DB_NAME="collectepro_db"
DB_USER="${DB_USERNAME:-collectepro_user}"

# Création du dossier de backup si inexistant
mkdir -p $BACKUP_DIR

# Exécution du dump SQL compressé
echo "[$(date)] Début de la sauvegarde PostgreSQL..."
docker exec -t $CONTAINER_NAME pg_dump -U $DB_USER $DB_NAME | gzip > $BACKUP_DIR/backup_${DATE}.sql.gz

if [ $? -eq 0 ]; then
    echo "[$(date)] Sauvegarde réussie : $BACKUP_DIR/backup_${DATE}.sql.gz"
else
    echo "[$(date)] ÉCHEC de la sauvegarde PostgreSQL !" >&2
    exit 1
fi

# Rétention : Suppression des sauvegardes de plus de 7 jours
echo "[$(date)] Nettoyage des anciennes sauvegardes (rétention 7 jours)..."
find $BACKUP_DIR -type f -name "*.sql.gz" -mtime +7 -delete

echo "[$(date)] Opération de sauvegarde terminée."
