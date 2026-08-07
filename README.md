# CollectPro

## 📋 Description du projet

CollectPro est une plateforme de collecte d'informations fonctionnant en mode
connecté et hors connexion. Elle permet aux organisations telles que les ONG,
les structures de santé ou les entreprises de réaliser des collectes de
données sur le terrain.

Les agents terrain saisissent leurs informations depuis une application
mobile, même sans connexion Internet. Dès que la connexion est disponible,
les données sont automatiquement synchronisées avec le serveur central. Les
superviseurs contrôlent et valident les collectes réalisées par leurs
équipes, tandis que les administrateurs gèrent l'ensemble de la plateforme.

## 🛠️ Technologies utilisées

### Backend

* Java 21
* Spring Boot
* Spring Security (JWT, BCrypt)
* Spring Data JPA
* PostgreSQL
* Docker / Docker Compose

### Mobile

* Flutter / Dart
* Riverpod
* Drift (SQLite)
* Dio (client HTTP)

### Dashboard Web

* Angular
* TypeScript
* Angular Material

## 🏗️ Architecture

Le projet suit une architecture client-serveur en trois briques :

```
Administrateur → Dashboard Angular → API REST → Spring Boot Backend → PostgreSQL
Agent / Superviseur → Application Flutter → SQLite Local → Synchronisation API
```

Le système est conçu selon une approche **Offline First** : les données sont
d'abord enregistrées localement (SQLite) puis synchronisées automatiquement
vers le serveur (PostgreSQL) dès qu'une connexion Internet est disponible.

Pour le détail complet (cas d'utilisation, modèle de données, diagrammes de
séquence), voir le dossier [`documentation/`](./documentation).

## 📁 Structure du dépôt

```
CollectPro/
├── backend/         # API REST Spring Boot
├── mobile/          # Application Flutter (agents / superviseurs)
├── dashboard/        # Dashboard web Angular (administrateurs)
├── documentation/    # Cahier des charges, diagrammes UML, specs
└── README.md
```

## 🚀 Installation

### Prérequis

|Composant|Outils requis|
|-|-|
|Backend|Java 21, Maven, PostgreSQL, Docker|
|Mobile|Flutter SDK, Android Studio, émulateur Android|
|Dashboard|Node.js LTS, Angular CLI|

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

### Mobile

```bash
cd mobile
flutter pub get
flutter run
```

### Dashboard

```bash
cd dashboard
npm install
ng serve
```

## 👥 Auteurs

* Équipe projet CollectPro

## 📄 Licence

À définir.



