# 🌊 Application Calanques de Marseille

Application mobile Android développée avec **Jetpack Compose** permettant de découvrir et de réserver des activités dans les calanques de Marseille. Ce projet a été réalisé dans le cadre du cursus **BTS SIO (SLAM)**.
### Par VILAIN Alexandre, ABOUTAYA Yahya, MARIE Lucas
## 🚀 Fonctionnalités

### 📍 Exploration
* **Accueil & Activités** : Liste complète des types d'activités (Kayak, Randonnée, Plongée) et des activités spécifiques récupérées via une API REST.
* **Détails de l'activité** : Consultation des descriptions, tarifs, durées et gestion dynamique du nombre de participants et de créneaux horaires.
* **Carte Interactive** : Visualisation géographique des calanques via OpenStreetMap (OSMDroid).

### 🛒 Réservation
* **Gestion du Panier** : Ajout et suppression d'activités avec calcul du prix total en temps réel via un `CartManager` centralisé.
* **Système de Réservation** : Validation du panier et envoi des données au serveur (gestion des erreurs 422, formats de date ISO 8601).

### 👤 Utilisateur
* **Authentification** : Système complet d'inscription (`Signup`) et de connexion (`Login`) avec gestion de token JWT persistant dans la session.
* **Espace Compte** : Consultation des informations personnelles récupérées via l'endpoint `/api/users/me` et gestion de la déconnexion.

## 🛠 Stack Technique

* **Langage** : Kotlin
* **UI** : Jetpack Compose (Material 3)
* **Navigation** : Compose Navigation (Système de routes centralisé dans `AppNavigation`)
* **Réseau** : Retrofit 2 + Kotlinx Serialization (JSON)
* **Images** : Coil (Chargement asynchrone des images distantes)
* **Cartographie** : OSMDroid
* **Architecture** : MVVM (Model-View-ViewModel) avec Coroutines pour l'asynchrone.

## 📁 Structure du Projet

```text
com.example.calanque
├── cart/               # Logique de gestion du panier (CartManager, CartItem)
├── models/             # Data classes (Activity, User, Availability, etc.)
├── navigation/         # Configuration des routes (Screen) et session utilisateur
└── screens/            # Écrans de l'application (Composables)
    ├── HomeScreen      # Liste des catégories
    ├── ActivitiesScreen# Liste des activités par type
    ├── PanierScreen    # Récapitulatif et validation des réservations
    ├── AuthScreens     # Connexion (Login) et Inscription (Signup)
    ├── AccountScreen   # Profil utilisateur
    └── ActivityDetail  # Détail, prix et sélection de créneaux
