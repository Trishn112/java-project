# AI-Based Movie Recommendation System

An intelligent, content-based movie recommendation web application built with **Java 21**, **Spring Boot 3.3.4**, **Thymeleaf**, **Vanilla CSS**, **The Movie Database (TMDB) API**, and **MySQL 8.0+ / JDBC**.

---

## 1. Project Overview

The system allows users to search for any movie title across any language (English, Hindi, Tamil, Telugu, Kannada, Malayalam, Korean, Japanese, Spanish, etc.) or explore live feeds, select the exact movie candidate by TMDB ID, and receive mathematically computed content-based recommendations.

### Key Features:
- **Clean, Responsive UI**:
  - Search input with quick suggestion chips.
  - Multi-candidate disambiguation grid when searches return multiple versions/sequels.
  - Target reference movie banner with original title, language tag, and TMDB rating.
  - Content similarity score percentage badges (separate from TMDB rating).
  - Algorithm breakdown (Genre Jaccard %, Plot Semantics %, Director/Cast %, Final Composite Score).
- **Explore Movies (TMDB Discover & Feeds)**:
  - **5 Distinct Feeds**: Now Playing in Theaters (`/movie/now_playing`), Popular Movies (`/movie/popular`), Upcoming Movies (`/movie/upcoming`), Trending Today (`/trending/movie/day`), and Recently Released (`/discover/movie`).
  - **Multi-Language Discovery**: Default "All Languages" with dedicated filters for Hindi, Tamil, Telugu, Kannada, Malayalam, Korean, Japanese, Spanish, French, German, and Chinese.
  - **Regional Filtering**: Supports India theatrical/regional releases (`region=IN`) and Global feeds.
  - **On-Demand Pagination**: Real-time pagination using TMDB's `page` and `total_pages` metadata.
  - **In-Memory Caching**: TTL-based cache preventing excessive redundant API requests.
- **Java Content-Based Recommendation Engine**:
  - Calculates multi-feature similarity using Genre Jaccard overlap, Plot Overview NLP semantics, and Cast/Director continuity.
  - Deduplicates recommendations and strictly excludes the target reference movie.
  - Combines content similarity (70% weight) with normalized rating (30% weight) for consistent ranking.
- **Decoupled Architecture & Resilience**:
  - Runs standalone out-of-the-box without requiring a database.
  - If MySQL 8.0+ is active, logs searches and recommendation history via atomic JDBC transactions.
  - Seamless fallback dataset with 40+ curated multi-lingual movies if TMDB network is offline.

---

## 2. Recommendation Algorithm Formula

The recommendation engine calculates a normalized composite ranking score for every candidate movie:

$$\text{FinalScore} = (0.70 \times \text{ContentSimilarity}) + (0.30 \times \text{NormalizedRating})$$

### 1. Multi-Feature Content Similarity:
$$\text{ContentSimilarity} = (0.50 \times \text{GenreSim}) + (0.30 \times \text{OverviewSim}) + (0.20 \times \text{CastDirectorSim})$$

- **Genre Similarity (50%)**: Jaccard index on genre sets:
  $$\text{GenreSim}(A, B) = \frac{|\text{Genres}_A \cap \text{Genres}_B|}{|\text{Genres}_A \cup \text{Genres}_B|}$$
- **Plot Overview & Keyword Semantics (30%)**: Normalized token overlap on clean keyword sets (after stop-word removal):
  $$\text{OverviewSim}(A, B) = \frac{|\text{Tokens}_A \cap \text{Tokens}_B|}{\sqrt{|\text{Tokens}_A| \times |\text{Tokens}_B|}}$$
- **Cast & Director Similarity (20%)**: Director continuity ($0.60$) and lead actor overlap ($0.40$).

### 2. Rating Normalization:
$$\text{NormalizedRating}(B) = \frac{\text{voteAverage}}{10.0}$$

### 3. Business Rules:
- The searched target movie is strictly **excluded** from recommendation results.
- Recommendations are deduplicated using TMDB IDs.
- Results are ranked in descending order of `FinalScore` using `java.util.Comparator`.

---

## 3. Java Programming Concepts Demonstrated

| Concept | Java Implementation |
| :--- | :--- |
| **Encapsulation** | `Movie.java`, `MovieRecommendation.java`, `SearchHistory.java`, `MoviePageResult.java` (private fields, accessors, validations). |
| **Interfaces & Abstraction** | `MovieRecommender`, `MovieRepository`, `SearchHistoryRepository`, `AbstractMovieRecommender`. |
| **Polymorphism** | Dynamic dispatch of recommendation strategies and repository implementations. |
| **Composition** | `MovieRecommendationService` composed of `TmdbApiClient`, `MovieRecommender`, and JDBC repositories. |
| **Collections & Generics** | `List<Movie>`, `Set<String>`, `Map<Integer, String>`, `HashSet`, `ArrayList`, `Comparator`. |
| **Exception Handling** | `MovieNotFoundException`, `TmdbApiException`, `DatabaseAccessException`. |
| **Multithreading** | `ExecutorService`, `CompletableFuture.allOf(...)` in `TmdbApiClient` for concurrent candidate fetching. |
| **JDBC & Transactions** | Native `Connection`, `PreparedStatement`, `ResultSet`, `commit()`, `rollback()` in `SearchHistoryJdbcRepository`. |

---

## 4. How to Configure TMDB Credentials

Set the `TMDB_API_KEY` environment variable or configure it in `application.properties`:

### In PowerShell:
```powershell
$env:TMDB_API_KEY="your_tmdb_api_key_or_access_token"
```

### In `application.properties`:
```properties
tmdb.api.key=${TMDB_API_KEY:}
```

*(Note: Secrets are never exposed in logs, templates, or client JavaScript)*.

---

## 5. How to Build and Run the Application

### Step 1: Run Automated Tests
```powershell
.\mvnw.cmd clean test
```

### Step 2: Launch the Web Application
```powershell
.\mvnw.cmd spring-boot:run
```

### Step 3: Open in Web Browser
Open your browser and navigate to:
👉 **`http://localhost:8080`**

---

## 6. MySQL Database Phase (Optional Integration)

When you are ready to enable MySQL persistence:

### 1. Database Creation
```sql
CREATE DATABASE IF NOT EXISTS movie_recommendation_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

### 2. Configure DataSource in PowerShell
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3306/movie_recommendation_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:SPRING_DATASOURCE_USERNAME="root"
$env:SPRING_DATASOURCE_PASSWORD="your_mysql_password"
```
The application will automatically initialize `movies`, `search_history`, and `recommendation_history` tables on startup via `schema.sql`.
